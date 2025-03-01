package guideme.libs.micromark.flow;

import guideme.libs.micromark.TestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class HeadingAtxTest {
    @ParameterizedTest(name = "[{index}] {2}")
    @CsvSource(delimiterString = "||", ignoreLeadingAndTrailingWhitespace = false, value = {
            "# foo||<h1>foo</h1>||should support a heading w/ rank 1",
            "## foo||<h2>foo</h2>||should support a heading w/ rank 2",
            "### foo||<h3>foo</h3>||should support a heading w/ rank 3",
            "#### foo||<h4>foo</h4>||should support a heading w/ rank 4",
            "##### foo||<h5>foo</h5>||should support a heading w/ rank 5",
            "###### foo||<h6>foo</h6>||should support a heading w/ rank 6",
            "####### foo||<p>####### foo</p>||should not support a heading w/ rank 7",
            "#5 bolt||<p>#5 bolt</p>||should not support a heading for a number sign not followed by whitespace (1)",
            "#hashtag||<p>#hashtag</p>||should not support a heading for a number sign not followed by whitespace (2)",
            "\\## foo||<p>## foo</p>||should not support a heading for an escaped number sign",
            "# foo *bar* \\*baz\\*||<h1>foo <em>bar</em> *baz*</h1>||should support text content in headings",
            "#                  foo                     ||<h1>foo</h1>||should support arbitrary initial and final whitespace",
            " ### foo||<h3>foo</h3>||should support an initial space",
            "  ## foo||<h2>foo</h2>||should support two initial spaces",
            "   # foo||<h1>foo</h1>||should support three initial spaces",
            "    # foo||<pre><code># foo^n</code></pre>||should not support four initial spaces",
            "foo^n    # bar||<p>foo^n# bar</p>||should not support four initial spaces when interrupting",
            "## foo ##||<h2>foo</h2>||should support a closing sequence (1)",
            "  ###   bar    ###||<h3>bar</h3>||should support a closing sequence (2)",
            "# foo ##################################||<h1>foo</h1>||should support a closing sequence w/ an arbitrary number of number signs (1)",
            "##### foo ##||<h5>foo</h5>||should support a closing sequence w/ an arbitrary number of number signs (2)",
            "### foo ###     ||<h3>foo</h3>||should support trailing whitespace after a closing sequence",
            "### foo ### b||<h3>foo ### b</h3>||should not support other content after a closing sequence",
            "# foo#||<h1>foo#</h1>||should not support a closing sequence w/o whitespace before it",
            "### foo \\###||<h3>foo ###</h3>||should not support an “escaped” closing sequence (1)",
            "## foo #\\##||<h2>foo ###</h2>||should not support an “escaped” closing sequence (2)",
            "# foo \\#||<h1>foo #</h1>||should not support an “escaped” closing sequence (3)",
            "****^n## foo^n****||<hr />^n<h2>foo</h2>^n<hr />||should support atx headings when not surrounded by blank lines",
            "Foo bar^n# baz^nBar foo||<p>Foo bar</p>^n<h1>baz</h1>^n<p>Bar foo</p>||should support atx headings interrupting paragraphs",
            "## ^n#^n### ###||<h2></h2>^n<h1></h1>^n<h3></h3>||should support empty atx headings",
            "> #^na||<blockquote>^n<h1></h1>^n</blockquote>^n<p>a</p>||should not support lazyness (1)",
            "> a^n#||<blockquote>^n<p>a</p>^n</blockquote>^n<h1></h1>||should not support lazyness (2)",
    })
    public void testGeneratedHtml(String markdown, String expectedHtml, String message) {
        TestUtil.assertGeneratedHtml(markdown, expectedHtml);
    }

    @Test
    public void testDisabled() {
        TestUtil.assertGeneratedHtmlWithDisabled("# a", "<p># a</p>", "should support turning off heading (atx)",
                "headingAtx");
    }

}
