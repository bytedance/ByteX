package com.ss.android.ugc.bytex.common.log.html;

public interface HtmlFragmentProvider {
    /**
     * provide a piece of HTML code
     * @return html code
     */
    String provide();

    void reset();
}
