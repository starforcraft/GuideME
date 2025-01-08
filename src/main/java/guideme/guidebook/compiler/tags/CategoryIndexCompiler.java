package guideme.guidebook.compiler.tags;

import guideme.guidebook.compiler.PageCompiler;
import guideme.guidebook.document.block.LytBlockContainer;
import guideme.guidebook.document.block.LytList;
import guideme.guidebook.document.block.LytListItem;
import guideme.guidebook.document.block.LytParagraph;
import guideme.guidebook.document.flow.LytFlowLink;
import guideme.guidebook.indices.CategoryIndex;
import guideme.libs.mdast.mdx.model.MdxJsxElementFields;
import java.util.Set;

public class CategoryIndexCompiler extends BlockTagCompiler {
    @Override
    public Set<String> getTagNames() {
        return Set.of("CategoryIndex");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {

        var category = el.getAttributeString("category", null);
        if (category == null) {
            parent.appendError(compiler, "Missing category", el);
            return;
        }

        var categories = compiler.getIndex(CategoryIndex.class).get(category);

        var list = new LytList(false, 0);
        for (var pageAnchor : categories) {
            var page = compiler.getPageCollection().getParsedPage(pageAnchor.pageId());

            var listItem = new LytListItem();
            var listItemPar = new LytParagraph();
            if (page == null) {
                listItemPar.appendText("Unknown page id: " + pageAnchor.pageId());
            } else {
                var link = new LytFlowLink();
                link.setClickCallback(guideScreen -> guideScreen.navigateTo(pageAnchor));
                link.appendText(page.getFrontmatter().navigationEntry().title());
                listItemPar.append(link);
            }
            listItem.append(listItemPar);
            list.append(listItem);
        }
        parent.append(list);
    }
}
