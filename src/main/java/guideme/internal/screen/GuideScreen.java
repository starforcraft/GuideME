package guideme.internal.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import guideme.Guide;
import guideme.GuidePage;
import guideme.PageAnchor;
import guideme.PageCollection;
import guideme.color.ColorValue;
import guideme.color.ConstantColor;
import guideme.color.SymbolicColor;
import guideme.compiler.AnchorIndexer;
import guideme.compiler.PageCompiler;
import guideme.compiler.ParsedGuidePage;
import guideme.document.DefaultStyles;
import guideme.document.LytPoint;
import guideme.document.LytRect;
import guideme.document.block.LytBlock;
import guideme.document.block.LytDocument;
import guideme.document.block.LytHeading;
import guideme.document.block.LytNode;
import guideme.document.block.LytParagraph;
import guideme.document.flow.LytFlowAnchor;
import guideme.document.flow.LytFlowContainer;
import guideme.document.flow.LytFlowContent;
import guideme.document.flow.LytFlowSpan;
import guideme.document.interaction.GuideTooltip;
import guideme.document.interaction.InteractiveElement;
import guideme.internal.GuideME;
import guideme.internal.GuideMEClient;
import guideme.internal.GuidebookText;
import guideme.internal.util.DashPattern;
import guideme.internal.util.DashedRectangle;
import guideme.layout.LayoutContext;
import guideme.layout.MinecraftFontMetrics;
import guideme.render.GuidePageTexture;
import guideme.render.SimpleRenderContext;
import guideme.style.TextAlignment;
import guideme.style.TextStyle;
import guideme.ui.GuideUiHost;
import guideme.ui.UiPoint;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuideScreen extends Screen implements GuideUiHost {
    private static final Logger LOG = LoggerFactory.getLogger(GuideScreen.class);

    // 20 virtual px margin around the document
    public static final int DOCUMENT_RECT_MARGIN = 20;
    private static final DashPattern DEBUG_NODE_OUTLINE = new DashPattern(1f, 4, 3, 0xFFFFFFFF, 500);
    private static final DashPattern DEBUG_CONTENT_OUTLINE = new DashPattern(0.5f, 2, 1, 0x7FFFFFFF, 500);
    private static final ColorValue DEBUG_HOVER_OUTLINE_COLOR = new ConstantColor(0x7FFFFF00);
    private static final ResourceLocation BACKGROUND_TEXTURE = GuideME.makeId("textures/guide/background.png");
    private final Guide guide;

    private final GuideScrollbar scrollbar;
    private final GuideScreenHistory history;
    private GuidePage currentPage;
    private final LytParagraph pageTitle;

    private Button backButton;
    private Button forwardButton;
    @Nullable
    private Screen returnToOnClose;

    /**
     * When the guidebook is initially opened, it does not do a proper layout due to missing width/height info. When we
     * should scroll to a point in the page, we have to "stash" that and do it after the initial proper layout has been
     * done.
     */
    @Nullable
    private String pendingScrollToAnchor;

    @Nullable
    private InteractiveElement mouseCaptureTarget;

    private GuideScreen(GuideScreenHistory history, Guide guide, PageAnchor anchor) {
        super(Component.literal("AE2 Guidebook"));
        this.history = history;
        this.guide = guide;
        this.scrollbar = new GuideScrollbar();
        this.pageTitle = new LytParagraph();
        this.pageTitle.setStyle(DefaultStyles.HEADING1);
        loadPageAndScrollTo(anchor);
    }

    /**
     * Opens and resets history.
     */
    public static GuideScreen openNew(Guide guide, PageAnchor anchor, GuideScreenHistory history) {
        history.push(anchor);

        return new GuideScreen(history, guide, anchor);
    }

    /**
     * Opens at current history position and only falls back to the index if the history is empty.
     */
    public static GuideScreen openAtPreviousPage(
            Guide guide,
            PageAnchor fallbackPage,
            GuideScreenHistory history) {
        var historyPage = history.current();
        if (historyPage.isPresent()) {
            return new GuideScreen(history, guide, historyPage.get());
        } else {
            return openNew(guide, fallbackPage, history);
        }
    }

    @Override
    protected void init() {
        super.init();

        updatePageLayout();

        // Add and re-position scrollbar
        addRenderableWidget(scrollbar);
        updateScrollbarPosition();

        GuideNavBar navbar = new GuideNavBar(this);
        addRenderableWidget(navbar);

        // Center them vertically in the margin
        backButton = new GuideIconButton(
                width - DOCUMENT_RECT_MARGIN - GuideIconButton.WIDTH * 3 - 10,
                2,
                GuideIconButton.Role.BACK,
                this::navigateBack);
        addRenderableWidget(backButton);
        forwardButton = new GuideIconButton(
                width - DOCUMENT_RECT_MARGIN - GuideIconButton.WIDTH * 2 - 5,
                2,
                GuideIconButton.Role.FORWARD,
                this::navigateForward);
        addRenderableWidget(forwardButton);
        var closeButton = new GuideIconButton(
                width - DOCUMENT_RECT_MARGIN - GuideIconButton.WIDTH,
                2,
                GuideIconButton.Role.CLOSE,
                this::onClose);
        addRenderableWidget(closeButton);
        updateTopNavButtons();
    }

    private void updateScrollbarPosition() {
        var docRect = getDocumentRect();
        scrollbar.move(docRect.right(), docRect.y(), docRect.height());
    }

    @Override
    public void tick() {
        processPendingScrollTo();

        // Tick all controls on the page
        if (currentPage != null) {
            tickNode(currentPage.document());
        }
    }

    private static void tickNode(LytNode node) {
        node.tick();

        for (var child : node.getChildren()) {
            tickNode(child);
        }
    }

    /**
     * If a scroll-to command is queued, this processes that.
     */
    private void processPendingScrollTo() {
        if (pendingScrollToAnchor == null) {
            return;
        }

        var anchor = pendingScrollToAnchor;
        pendingScrollToAnchor = null;

        var indexer = new AnchorIndexer(currentPage.document());

        var targetAnchor = indexer.get(anchor);
        if (targetAnchor == null) {
            LOG.warn("Failed to find anchor {} in page {}", anchor, currentPage);
            return;
        }

        if (targetAnchor.flowContent() instanceof LytFlowAnchor flowAnchor && flowAnchor.getLayoutY().isPresent()) {
            scrollbar.setScrollAmount(flowAnchor.getLayoutY().getAsInt());
        } else {
            var bounds = targetAnchor.blockNode().getBounds();
            scrollbar.setScrollAmount(bounds.y());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateTopNavButtons();

        renderSkyStoneBackground(guiGraphics);

        // Set scissor rectangle to rect that we show the document in
        var documentRect = getDocumentRect();

        guiGraphics.fill(documentRect.x(), documentRect.y(), documentRect.right(), documentRect.bottom(), 0x80333333);

        // Move rendering to anchor @ 0,0 in the document rect
        var documentViewport = getDocumentViewport();
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(documentRect.x() - documentViewport.x(), documentRect.y() - documentViewport.y(), 0);

        var document = currentPage.document();
        var context = new SimpleRenderContext(documentViewport, guiGraphics);

        guiGraphics.enableScissor(documentRect.x(), documentRect.y(), documentRect.right(), documentRect.bottom());

        // Render all text content in one large batch to improve performance
        var buffers = context.beginBatch();
        document.renderBatch(context, buffers);
        context.endBatch(buffers);

        document.render(context);

        guiGraphics.disableScissor();

        if (GuideMEClient.instance().isShowDebugGuiOverlays()) {
            renderHoverOutline(document, context);
        }

        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0, 200);

        renderTitle(documentRect, context);

        renderExternalPageSource(documentRect, context);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        poseStack.popPose();

        // Render tooltip
        if (document.getHoveredElement() != null) {
            renderTooltip(guiGraphics, mouseX, mouseY);
        }

    }

    private void renderExternalPageSource(LytRect documentRect, SimpleRenderContext context) {
        // Render the source of the content
        var externalSource = getExternalSourceName();
        if (externalSource != null) {
            var paragraph = new LytParagraph();
            paragraph.appendText(GuidebookText.ContentFrom.text().getString() + " ");
            var sourceSpan = new LytFlowSpan();

            sourceSpan.appendText(externalSource);
            sourceSpan.setStyle(TextStyle.builder().italic(true).build());
            paragraph.append(sourceSpan);
            paragraph.setStyle(TextStyle.builder().alignment(TextAlignment.RIGHT).build());
            var layoutContext = new LayoutContext(new MinecraftFontMetrics());
            paragraph.layout(layoutContext, documentRect.x(), documentRect.bottom(), documentRect.width());
            var buffers = context.beginBatch();
            paragraph.renderBatch(context, buffers);
            context.endBatch(buffers);
        }
    }

    /**
     * Gets a readable name for the source of the page (i.e. resource pack name, mod name) if the page has been
     * contributed externally.
     */
    @Nullable
    private String getExternalSourceName() {
        var sourcePackId = currentPage.sourcePack();
        // If the pages came directly from a mod resource pack, we have to use the mod-list to resolve its name
        if (sourcePackId.startsWith("mod:") || sourcePackId.startsWith("mod/")) {
            var modId = sourcePackId.substring("mod:".length());

            // Only show the source marker for pages that are not native to the guides mod
            if (guide.getDefaultNamespace().equals(modId)) {
                return null;
            }

            return ModList.get().getModContainerById(modId)
                    .map(ModContainer::getModInfo)
                    .map(IModInfo::getDisplayName)
                    .orElse(null);
        }

        // Only show the source marker for pages that are not native to the guides mod
        if (guide.getDefaultNamespace().equals(sourcePackId)) {
            return null;
        }

        var pack = Minecraft.getInstance().getResourcePackRepository().getPack(sourcePackId);
        if (pack != null) {
            return pack.getDescription().getString();
        }

        return null;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Stub this out otherwise vanilla renders a background on top of our content
    }

    private void renderTitle(LytRect documentRect, SimpleRenderContext context) {
        var buffers = context.beginBatch();
        pageTitle.renderBatch(context, buffers);
        context.endBatch(buffers);
        context.fillRect(
                documentRect.x(),
                documentRect.y() - 1,
                documentRect.width(),
                1,
                SymbolicColor.HEADER1_SEPARATOR);
    }

    private void renderSkyStoneBackground(GuiGraphics guiGraphics) {
        RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
        guiGraphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        var docPos = getDocumentPoint(x, y);
        if (docPos == null) {
            return;
        }
        var hoveredElement = currentPage.document().getHoveredElement();
        if (hoveredElement != null) {
            dispatchInteraction(
                    hoveredElement,
                    el -> el.getTooltip(docPos.x(), docPos.y()))
                    .ifPresent(tooltip -> renderTooltip(guiGraphics, tooltip, x, y));
        }
    }

    private static void renderHoverOutline(LytDocument document, SimpleRenderContext context) {
        var hoveredElement = document.getHoveredElement();
        if (hoveredElement != null) {
            // Fill a rectangle highlighting margins
            if (hoveredElement.node() instanceof LytBlock block) {
                var bounds = block.getBounds();
                if (block.getMarginTop() > 0) {
                    context.fillRect(
                            bounds.withHeight(block.getMarginTop()).move(0, -block.getMarginTop()),
                            DEBUG_HOVER_OUTLINE_COLOR);
                }
                if (block.getMarginBottom() > 0) {
                    context.fillRect(
                            bounds.withHeight(block.getMarginBottom()).move(0, bounds.height()),
                            DEBUG_HOVER_OUTLINE_COLOR);
                }
                if (block.getMarginLeft() > 0) {
                    context.fillRect(
                            bounds.withWidth(block.getMarginLeft()).move(-block.getMarginLeft(), 0),
                            DEBUG_HOVER_OUTLINE_COLOR);
                }
                if (block.getMarginRight() > 0) {
                    context.fillRect(
                            bounds.withWidth(block.getMarginRight()).move(bounds.width(), 0),
                            DEBUG_HOVER_OUTLINE_COLOR);
                }
            }

            // Fill the content rectangle
            DashedRectangle.render(context.poseStack(), hoveredElement.node().getBounds(), DEBUG_NODE_OUTLINE, 0);

            // Also outline any inline-elements in the block
            if (hoveredElement.content() != null) {
                if (hoveredElement.node() instanceof LytFlowContainer flowContainer) {
                    flowContainer.enumerateContentBounds(hoveredElement.content())
                            .forEach(bound -> {
                                DashedRectangle.render(context.poseStack(), bound, DEBUG_CONTENT_OUTLINE, 0);
                            });
                }
            }

            // Render the class-name of the hovered node to make it easier to identify
            var bounds = hoveredElement.node().getBounds();
            context.renderText(
                    hoveredElement.node().getClass().getName(),
                    DefaultStyles.BASE_STYLE,
                    bounds.x(),
                    bounds.bottom());
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);

        if (mouseCaptureTarget != null) {
            var docPointUnclamped = getDocumentPointUnclamped(mouseX, mouseY);
            mouseCaptureTarget.mouseMoved(this, docPointUnclamped.x(), docPointUnclamped.y());
        }

        var docPoint = getDocumentPoint(mouseX, mouseY);
        if (docPoint != null) {
            dispatchEvent(docPoint.x(), docPoint.y(), el -> {
                return el.mouseMoved(this, docPoint.x(), docPoint.y());
            });
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        var docPoint = getDocumentPoint(mouseX, mouseY);
        if (docPoint != null) {
            if (button == 3) {
                navigateBack();
            } else if (button == 4) {
                navigateForward();
            }

            return dispatchEvent(docPoint.x(), docPoint.y(), el -> {
                return el.mouseClicked(this, docPoint.x(), docPoint.y(), button);
            });
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (super.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }

        if (mouseCaptureTarget != null) {
            var currentTarget = mouseCaptureTarget;

            var docPointUnclamped = getDocumentPointUnclamped(mouseX, mouseY);
            currentTarget.mouseReleased(this, docPointUnclamped.x(), docPointUnclamped.y(), button);

            releaseMouseCapture(currentTarget);
        }

        var docPoint = getDocumentPoint(mouseX, mouseY);
        if (docPoint != null) {
            return dispatchEvent(docPoint.x(), docPoint.y(), el -> {
                return el.mouseReleased(this, docPoint.x(), docPoint.y(), button);
            });
        } else {
            return false;
        }
    }

    @Override
    public void navigateTo(ResourceLocation pageId) {
        navigateTo(new PageAnchor(pageId, null));
    }

    @Override
    public void navigateTo(PageAnchor anchor) {
        if (currentPage.id().equals(anchor.pageId())) {
            pendingScrollToAnchor = anchor.anchor();
            if (anchor.anchor() != null) {
                history.push(anchor);
            }
            return;
        }

        loadPageAndScrollTo(anchor);
        history.push(anchor);
    }

    // Navigate to next page in history (only possible if we've navigated back previously)
    private void navigateForward() {
        history.forward().ifPresent(this::loadPageAndScrollTo);
    }

    // Navigate to previous page in history
    private void navigateBack() {
        history.back().ifPresent(this::loadPageAndScrollTo);
    }

    private void loadPageAndScrollTo(PageAnchor anchor) {
        loadPage(anchor.pageId());

        scrollbar.setScrollAmount(0);
        updatePageLayout();

        pendingScrollToAnchor = anchor.anchor();
    }

    @Override
    public void reloadPage() {
        loadPage(currentPage.id());
        updatePageLayout();
    }

    private void loadPage(ResourceLocation pageId) {
        closePage();

        GuidePageTexture.releaseUsedTextures();
        var page = guide.getParsedPage(pageId);

        if (page == null) {
            // Build a "not found" page dynamically
            page = buildNotFoundPage(pageId);
        }

        currentPage = PageCompiler.compile(guide, guide.getExtensions(), page);

        // Find and pull out the first heading
        pageTitle.clearContent();
        for (var flowContent : extractPageTitle(currentPage)) {
            pageTitle.append(flowContent);
        }
    }

    private void closePage() {
        // Reset previously stored interactive elements
        if (mouseCaptureTarget != null) {
            releaseMouseCapture(mouseCaptureTarget);
        }
    }

    private Iterable<LytFlowContent> extractPageTitle(GuidePage page) {
        for (var block : page.document().getBlocks()) {
            if (block instanceof LytHeading heading) {
                if (heading.getDepth() == 1) {
                    page.document().removeChild(heading);
                    return heading.getContent();
                } else {
                    break; // Any heading other than depth 1 cancels this algo
                }
            }
        }
        return List.of();
    }

    private ParsedGuidePage buildNotFoundPage(ResourceLocation pageId) {
        String pageSource = "# Page not Found\n" +
                "\n" +
                "Page \"" + pageId + "\" could not be found.";

        return PageCompiler.parse(
                pageId.getNamespace(),
                pageId,
                pageSource);
    }

    @Override
    public void removed() {
        super.removed();
        GuidePageTexture.releaseUsedTextures();
    }

    /**
     * Sets a screen to return to when closing this guide.
     */
    public void setReturnToOnClose(@Nullable Screen screen) {
        this.returnToOnClose = screen;
    }

    @Override
    public void openUrl(String href) {
        URI uri;
        try {
            uri = URI.create(href);
        } catch (IllegalArgumentException ignored) {
            LOG.debug("Can't parse '{}' as URL in '{}'", href, currentPage);
            return;
        }

        // Treat it as an external URL if it has a scheme
        if (uri.getScheme() != null) {
            if (minecraft.options.chatLinksPrompt().get().booleanValue()) {
                this.minecraft.setScreen(new ConfirmLinkScreen(doOpen -> {
                    if (doOpen) {
                        Util.getPlatform().openUri(uri);
                    }
                    this.minecraft.setScreen(this);
                }, href, false));
            } else {
                Util.getPlatform().openUri(uri);
            }
        } else {
            // Otherwise treat it as a page anchor
            var pageId = GuideME.makeId(uri.getSchemeSpecificPart());
            PageAnchor anchor = new PageAnchor(pageId, uri.getFragment());
            history.push(anchor);
            loadPageAndScrollTo(anchor);
        }
    }

    @FunctionalInterface
    interface EventInvoker {
        boolean invoke(InteractiveElement el);
    }

    private boolean dispatchEvent(int x, int y, EventInvoker invoker) {
        return dispatchInteraction(x, y, el -> {
            if (invoker.invoke(el)) {
                return Optional.of(true);
            } else {
                return Optional.empty();
            }
        }).orElse(false);
    }

    private <T> Optional<T> dispatchInteraction(int x, int y, Function<InteractiveElement, Optional<T>> invoker) {
        var underCursor = currentPage.document().pick(x, y);
        if (underCursor != null) {
            return dispatchInteraction(underCursor, invoker);
        }

        return Optional.empty();
    }

    private static <T> Optional<T> dispatchInteraction(LytDocument.HitTestResult receiver,
            Function<InteractiveElement, Optional<T>> invoker) {
        // Iterate through content ancestors
        for (var el = receiver.content(); el != null; el = el.getFlowParent()) {
            if (el instanceof InteractiveElement interactiveEl) {
                var result = invoker.apply(interactiveEl);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        // Iterate through node ancestors
        for (var node = receiver.node(); node != null; node = node.getParent()) {
            if (node instanceof InteractiveElement interactiveEl) {
                var result = invoker.apply(interactiveEl);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public void afterMouseMove() {
        super.afterMouseMove();

        var mouseHandler = minecraft.mouseHandler;
        var scale = (double) minecraft.getWindow().getGuiScaledWidth()
                / (double) minecraft.getWindow().getScreenWidth();
        var x = mouseHandler.xpos() * scale;
        var y = mouseHandler.ypos() * scale;

        // If there's a widget under the cursor, ignore document hit-testing
        var document = currentPage.document();
        if (getChildAt(x, y).isPresent()) {
            document.setHoveredElement(null);
            return;
        }

        var docPoint = getDocumentPoint(x, y);
        if (docPoint != null) {
            var hoveredEl = document.pick(docPoint.x(), docPoint.y());
            document.setHoveredElement(hoveredEl);
        } else {
            document.setHoveredElement(null);
        }
    }

    @Override
    public @Nullable UiPoint getDocumentPoint(double screenX, double screenY) {
        var documentRect = getDocumentRect();

        if (screenX >= documentRect.x() && screenX < documentRect.right()
                && screenY >= documentRect.y() && screenY < documentRect.bottom()) {
            return getDocumentPointUnclamped(screenX, screenY);
        }

        return null; // Outside the document
    }

    @Override
    public UiPoint getDocumentPointUnclamped(double screenX, double screenY) {
        var documentRect = getDocumentRect();
        var docX = (int) Math.round(screenX - documentRect.x());
        var docY = (int) Math.round(screenY + scrollbar.getScrollAmount() - documentRect.y());
        return new UiPoint(docX, docY);
    }

    /**
     * Translate a point from within the document into the screen coordinate system.
     */
    @Override
    public LytPoint getScreenPoint(LytPoint documentPoint) {
        var documentRect = getDocumentRect();
        var documentViewport = getDocumentViewport();
        var x = documentPoint.x() - documentViewport.x();
        var y = documentPoint.y() - documentViewport.y();
        return new LytPoint(
                documentRect.x() + x,
                documentRect.y() + y);
    }

    @Override
    public LytRect getDocumentRect() {
        var margin = DOCUMENT_RECT_MARGIN;

        // The page title may need more space than the default margin provides
        var marginTop = Math.max(
                margin,
                5 + pageTitle.getBounds().height());

        return new LytRect(
                margin,
                marginTop,
                width - 2 * margin,
                height - margin - marginTop);
    }

    @Override
    public LytRect getDocumentViewport() {
        var documentRect = getDocumentRect();
        return new LytRect(0, scrollbar.getScrollAmount(), documentRect.width(), documentRect.height());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
            return scrollbar.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
        return true;
    }

    private void renderTooltip(GuiGraphics guiGraphics, GuideTooltip tooltip, int mouseX, int mouseY) {
        var minecraft = Minecraft.getInstance();
        var clientLines = tooltip.getLines();

        if (clientLines.isEmpty()) {
            return;
        }

        int frameWidth = 0;
        int frameHeight = clientLines.size() == 1 ? -2 : 0;

        for (var clientTooltipComponent : clientLines) {
            frameWidth = Math.max(frameWidth, clientTooltipComponent.getWidth(minecraft.font));
            frameHeight += clientTooltipComponent.getHeight();
        }

        if (!tooltip.getIcon().isEmpty()) {
            frameWidth += 18;
            frameHeight = Math.max(frameHeight, 18);
        }

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + frameWidth > this.width) {
            x -= 28 + frameWidth;
        }

        if (y + frameHeight + 6 > this.height) {
            y = this.height - frameHeight - 6;
        }

        int zOffset = 400;

        TooltipRenderUtil.renderTooltipBackground(guiGraphics, x, y, frameWidth, frameHeight, zOffset);

        if (!tooltip.getIcon().isEmpty()) {
            x += 18;
        }

        var poseStack = guiGraphics.pose();
        var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(0.0, 0.0, zOffset);
        int currentY = y;

        // Batch-render tooltip text first
        for (int i = 0; i < clientLines.size(); ++i) {
            var line = clientLines.get(i);
            line.renderText(minecraft.font, x, currentY, poseStack.last().pose(), bufferSource);
            currentY += line.getHeight() + (i == 0 ? 2 : 0);
        }

        bufferSource.endBatch();

        // Then render tooltip decorations, items, etc.
        currentY = y;
        if (!tooltip.getIcon().isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0, 0, zOffset);
            guiGraphics.renderItem(tooltip.getIcon(), x - 18, y);
            poseStack.popPose();
        }

        for (int i = 0; i < clientLines.size(); ++i) {
            var line = clientLines.get(i);
            line.renderImage(minecraft.font, x, currentY, guiGraphics);
            currentY += line.getHeight() + (i == 0 ? 2 : 0);
        }
        poseStack.popPose();
    }

    private void updatePageLayout() {
        // Update layout of page title, since it's used for the document rectangle
        updateTitleLayout();

        var docViewport = getDocumentViewport();
        var context = new LayoutContext(new MinecraftFontMetrics());

        // Build layout if needed
        var document = currentPage.document();
        document.updateLayout(context, docViewport.width());
        scrollbar.setContentHeight(document.getContentHeight());
    }

    private void updateTitleLayout() {
        var context = new LayoutContext(new MinecraftFontMetrics());
        // Compute the fake layout to find out how high it would be
        int availableWidth = width;

        // Remove the document viewport margin
        availableWidth -= 2 * DOCUMENT_RECT_MARGIN;

        // Account for the navigation buttons on the right
        availableWidth -= GuideIconButton.WIDTH * 2 + 5;

        // Remove 2 * 5 as margin
        availableWidth -= 10;

        if (availableWidth < 0) {
            availableWidth = 0;
        }

        pageTitle.layout(context, 0, 0, availableWidth);
        var height = pageTitle.getBounds().height();

        // Now compute the real layout
        var documentRect = getDocumentRect();

        int titleY = (documentRect.y() - height) / 2;

        pageTitle.layout(context, documentRect.x() + 5, titleY, availableWidth);

        updateScrollbarPosition();
    }

    public ResourceLocation getCurrentPageId() {
        return currentPage.id();
    }

    private void updateTopNavButtons() {
        backButton.active = history.peekBack().isPresent();
        forwardButton.active = history.peekForward().isPresent();
    }

    @Override
    public PageCollection getGuide() {
        return guide;
    }

    @Override
    public @Nullable InteractiveElement getMouseCaptureTarget() {
        return mouseCaptureTarget;
    }

    @Override
    public void captureMouse(InteractiveElement element) {
        if (mouseCaptureTarget != element) {
            if (mouseCaptureTarget != null) {
                releaseMouseCapture(mouseCaptureTarget);
            }
            mouseCaptureTarget = element;
        }
    }

    @Override
    public void releaseMouseCapture(InteractiveElement element) {
        if (mouseCaptureTarget == element) {
            mouseCaptureTarget = null;
            element.mouseCaptureLost();
            if (mouseCaptureTarget != null) {
                throw new IllegalStateException("Element " + element + " recaptured the mouse in its release event");
            }
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.screen == this && this.returnToOnClose != null) {
            minecraft.setScreen(this.returnToOnClose);
            this.returnToOnClose = null;
            return;
        }
        closePage();
        super.onClose();
    }
}
