package guideme.libs.mdast.mdx.model;

import com.google.gson.stream.JsonWriter;
import guideme.libs.mdast.model.MdAstFlowContent;
import guideme.libs.mdast.model.MdAstParent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class MdxJsxFlowElement extends MdAstParent<MdAstFlowContent> implements MdxJsxElementFields, MdAstFlowContent {
    public String name;
    public List<MdxJsxAttributeNode> attributes;

    public MdxJsxFlowElement() {
        this("", new ArrayList<>());
    }

    public MdxJsxFlowElement(String name, List<MdxJsxAttributeNode> attributes) {
        super("mdxJsxFlowElement");
        this.name = name;
        this.attributes = attributes;
    }

    @Override
    public @Nullable String name() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<MdxJsxAttributeNode> attributes() {
        return attributes;
    }

    @Override
    protected Class<MdAstFlowContent> childClass() {
        return MdAstFlowContent.class;
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        super.writeJson(writer);
        writer.name("name").value(name);
        writer.name("attributes");
        writer.beginArray();
        for (var attribute : attributes) {
            attribute.toJson(writer);
        }
        writer.endArray();
    }
}
