package guideme.compiler.tags;

import guideme.PageAnchor;
import guideme.compiler.PageCompiler;
import guideme.document.block.AlignItems;
import guideme.document.block.LytBlock;
import guideme.document.block.LytBlockContainer;
import guideme.document.block.LytHBox;
import guideme.document.block.LytList;
import guideme.document.block.LytListItem;
import guideme.document.block.LytParagraph;
import guideme.document.flow.LytFlowLink;
import guideme.libs.mdast.mdx.model.MdxJsxElementFields;
import guideme.navigation.NavigationNode;
import guideme.scene.LytItemImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public class SubPagesCompiler extends BlockTagCompiler {
    private static final Comparator<NavigationNode> ALPHABETICAL_COMPARATOR = Comparator
            .comparing(NavigationNode::title);

    @Override
    public Set<String> getTagNames() {
        return Set.of("SubPages");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        var pageIdStr = el.getAttributeString("id", null);
        var showIcons = MdxAttrs.getBoolean(compiler, parent, el, "icons", false);
        var alphabetical = MdxAttrs.getBoolean(compiler, parent, el, "alphabetical", false);

        var navigationTree = compiler.getPageCollection().getNavigationTree();

        // Find the page in the tree, if it's explicitly set to empty, show the root nav
        List<NavigationNode> subNodes;
        if ("".equals(pageIdStr)) {
            subNodes = navigationTree.getRootNodes();
        } else {
            ResourceLocation pageId;
            try {
                pageId = pageIdStr == null ? compiler.getPageId() : compiler.resolveId(pageIdStr);
            } catch (Exception e) {
                parent.appendError(compiler, "Invalid id", el);
                return;
            }

            var node = navigationTree.getNodeById(pageId);
            if (node == null) {
                parent.appendError(compiler, "Couldn't find page " + pageId + " in the navigation tree", el);
                return; // Not found in navigation tree
            }

            subNodes = node.children();
        }

        if (alphabetical) {
            subNodes = new ArrayList<>(subNodes);
            subNodes.sort(ALPHABETICAL_COMPARATOR);
        }

        var list = new LytList(false, 0);
        for (var childNode : subNodes) {
            if (!childNode.hasPage()) {
                continue;
            }

            var listItem = new LytListItem();
            var listItemPar = new LytParagraph();

            var link = new LytFlowLink();
            link.setClickCallback(guideScreen -> guideScreen.navigateTo(PageAnchor.page(childNode.pageId())));
            link.appendText(childNode.title());
            listItemPar.append(link);

            LytBlock listItemBlock = listItemPar;

            if (showIcons && !childNode.icon().isEmpty()) {
                var lytHBox = new LytHBox();

                var icon = new LytItemImage();
                icon.setItem(childNode.icon());
                lytHBox.append(icon);
                lytHBox.append(listItemPar);
                lytHBox.setAlignItems(AlignItems.CENTER);
                listItemBlock = lytHBox;
            }

            listItem.append(listItemBlock);
            list.append(listItem);
        }
        parent.append(list);
    }
}
