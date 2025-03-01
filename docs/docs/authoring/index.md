---
description: How to create content for a GuideME guide.
---

# Authoring Pages

Pages for a guidebook are read from *all resource packs* across *all namespace*.
That is why each guidebook has its own unique subdirectory, which by default
is `guides/<guide_id_namespace>/<guide_id_path>`. For a guidebook with the id `mod:guide`, this would be
`guides/mod/guide`.
Each file with the extension `.md` in this directory and any subdirectory is considered a page.

:::note

Like all files in Minecraft resource packs, page filenames must be [valid resource ids](https://minecraft.wiki/w/Resource_location).
Your filenames must all be lowercase, for example.

:::

Pages are written in Markdown and follow the [Commonmark](https://commonmark.org/) specification.
We also support [Github Tables](https://github.github.com/gfm/#tables-extension-).

Every page should usually declare its title as a level 1 heading at the start (`# Page Title`).

## Frontmatter

Every page can have a header ("frontmatter") that defines metadata for the page in YAML format.

Example:

```yaml
---
navigation:
  title: Page Title
---

# Page Title

Content
```

## Adding Pages to the Navigation Bar

To include a page in the navigation sidebar, it needs to define the `navigation` key in its frontmatter as such:

```yaml
---
navigation:
  # Title shown in the navigation bar
  title: Page Title
  # [OPTIONAL] Item ID for an icon 
  # defaults to the same namespace as the pages, so ae2 in our guidebook
  icon: debug_card
  # [OPTIONAL] The page ID of the parent this page should be sorted under as a child entry
  # If it's in the same namespace as the current page, the namespace can be omitted, otherwise use "ae2:path/to/file.md"
  parent: getting-started.md
---
```

## Declaring Pages as ItemLink targets

When using the `<ItemLink ... />` tag, the guidebook will try to find the page that explains what the given item does.

For this it searches all pages for the `item_ids` frontmatter key. If a page you write should be the primary page
for an item, list it in the `item_ids` frontmatter as such:

```yaml
---
item_ids:
  - ae2:item_id
  - ae2:other_item_id
---
```

Using `<ItemLink id="item_id" />` or `<ItemLink id="ae2:item_id" />` will then link to this page, as will slots
in recipes that show that item.

## Using Images

To show an image, just put it (.png or .jpg) in the `guidebook/assets` folder and embed it either:

* Using a normal Markdown image
* Using `<FloatingImage src="path/to/image.png" align="left or right" />` to have text wrap around the image.
  Use align="left" to wrap text on the right and align="right" to wrap text on the left of the image.
  To insert a break that prevents further text from wrapping from all previous floating images,
  use `<br clear="all" />`.

## Custom Tags

The following custom tags are supported in our Markdown pages.

In all custom tags, item and page ids by default inherit the namespace of the page they're on. So if the
page is in AE2s guidebook, all ids automatically use the `ae2` namespace, unless specified.

### Column / Row Layout

To lay out other tags (such as item images) in a row or column, use the `<Row></Row>`
and `<Column></Column>` tags. You can set a custom gap between items using the `gap` attribute.
It defaults to 5.

Example:

```markdown
<Row>
  <ItemImage id="interface" />
  <ItemImage id="stick" />
</Row>
```

### Item Links

To automatically show the translated item name, including an appropriate tooltip, and have the item name link to the
primary guidebook page for that item, use the  `<ItemLink id="item_id" />` tag. The id can omit the `ae2` namespace.

[Pages need to be set as the primary target for certain item ids manually](#declaring-pages-as-itemlink-targets).

### Recipes

To show the recipes used to create a certain item, use the `<RecipeFor id="item_id" />` tag.

To show a specific recipe, use the `<Recipe id="recipe/id" />` tag.

### Item Grids

To show-case multiple related items in a grid-layout, use the following markup:

```markdown
<ItemGrid>
  <ItemIcon id="interface" />
  <ItemIcon id="cable_interface" />
</ItemGrid>
```

### Category Index

Pages can further be assigned to be part of multiple categories (orthogonal to the navigation bar).

To do so, specify the following frontmatter key:

```yaml
---
categories:
  - Category 1
  - Category 2
  - Category 3
---
```

A category can contain an unlimited number of pages.

To automatically show a table of contents for a category, use the `<CategoryIndex category="Category 1" />` tag,
and specify the name of the category. It will then display a list of all pages that declare to be part of that
category.

### Sub Pages

This tag will show a list of links to pages. The list will be sourced from the child-pages of
the current page in the navigation-tree. If a specific page-id is given in the `id` attribute, the child-pages of that
page will be shown instead.

The list can be sorted alphabetically (by title) by adding `alphabetical={true}`.

To show the icons associated with each navigation-node, supply `icons={true}`. This does not look very appealing if
some child-pages have icons and others don't.

### Item Images

To show an item, use:

```
<ItemImage id="mod:item_id" />
```

IDs from your own mod don't need to be qualified with the mod id.

The tag also supports the following attributes:

| Attribute | Description                                                                                                                                       |
|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| scale     | Allows the item image to be scaled. Supports floating point numbers. `scale="1.5"` will show the item at 150% of its natural size.                |
| float     | Allows the item image to be floated like  `FloatingImage` to make it show to the left or right with a block of text. (Allows values: left, right) |

### Block Images

To show a 3d rendering of a block, use:

```
<BlockImage id="mod:block_id" />
```

IDs from your own mod don't need to be qualified with the mod id.

The tag also supports the following attributes:

| Attribute   | Description                                                                                                                                                                                     |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| scale       | Allows the block image to be scaled. Supports floating point numbers. `scale="1.5"` will show at 150% of its normal size.                                                                       |
| float       | Allows the block image to be floated like `FloatingImage` to make it show to the left or right with a block of text. (Allows values: left, right)                                               |
| perspective | Allows the orientation of the block to be changed. By default, the north-east corner of the block will be facing forward. Allowed values: isometric-north-east (default), isometric-north-west. |
| `p:<name>`  | Allows setting arbitrary block state properties on the rendered block, where `<name>` is the name of a block state property.                                                                    |
