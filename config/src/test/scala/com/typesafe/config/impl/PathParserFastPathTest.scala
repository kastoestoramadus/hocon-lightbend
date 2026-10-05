/*
 *   Copyright (C) 2011-2026 Typesafe Inc. <http://typesafe.com>
 */
package com.typesafe.config.impl

import org.junit.Assert._
import org.junit._

class PathParserFastPathTest extends TestUtils {

    private def assertFastPathAgreesWithFullParser(s: String): Unit = {
        val fast = PathParser.speculativeFastParsePath(s)
        assertTrue(s"expected '$s' to take the fast path", fast != null)
        val full = PathParser.parsePath(s)
        assertEquals(s"fast and full parser disagree on '$s'", full, fast)
    }

    @Test
    def digitInMiddleOfKeyTakesFastPath() {
        // #857: a digit anywhere in a key made every read fall back to the
        // full tokenizer, ~10x slower reads for keys like "prop50"
        assertFalse(PathParser.looksUnsafeForFastParser("prop50"))
        assertFastPathAgreesWithFullParser("prop50")
        assertFastPathAgreesWithFullParser("oauth2Client")
        assertFastPathAgreesWithFullParser("s3Bucket")
        assertFastPathAgreesWithFullParser("worker1.name")
        assertFastPathAgreesWithFullParser("a1b2.c3d4")
    }

    @Test
    def digitLeadingElementsStillTakeSlowPath() {
        // the full parser normalizes number tokens; elements that start
        // with a digit keep going through the full parser
        assertTrue(PathParser.looksUnsafeForFastParser("50cent"))
        assertTrue(PathParser.looksUnsafeForFastParser("1e5"))
        assertTrue(PathParser.looksUnsafeForFastParser("a.1b"))
        assertTrue(PathParser.looksUnsafeForFastParser("a.2"))
    }

    @Test
    def fastPathStillRejectsUnsafePaths() {
        assertTrue(PathParser.looksUnsafeForFastParser(""))
        assertTrue(PathParser.looksUnsafeForFastParser(".foo"))
        assertTrue(PathParser.looksUnsafeForFastParser("foo."))
        assertTrue(PathParser.looksUnsafeForFastParser("foo..bar"))
        assertTrue(PathParser.looksUnsafeForFastParser("foo bar"))
        assertTrue(PathParser.looksUnsafeForFastParser("foo\"bar"))
        assertTrue(PathParser.looksUnsafeForFastParser("-foo"))
    }

    @Test
    def fastAndFullParserAgreeOnSafePaths() {
        val safe = Seq("foo", "foo.bar", "foo-bar", "foo-", "foo_bar", "a1",
            "prop50.s3Bucket", "http2.enabled", "sha256.sum")
        for (s <- safe) assertFastPathAgreesWithFullParser(s)
    }
}
