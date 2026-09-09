package com.example.util

object PageScriptsHelper {

    val DARK_MODE_SCRIPT = """
        (function() {
            var styleId = 'nora-injected-dark-mode';
            var existing = document.getElementById(styleId);
            if (existing) {
                existing.remove();
                return 'disabled';
            } else {
                var style = document.createElement('style');
                style.id = styleId;
                style.textContent = `
                    html { 
                        filter: invert(90%) hue-rotate(180deg) !important; 
                        background: #121212 !important; 
                    }
                    img, video, canvas, svg, [style*="background-image"] { 
                        filter: invert(100%) hue-rotate(180deg) !important; 
                    }
                `;
                document.head.appendChild(style);
                return 'enabled';
            }
        })();
    """.trimIndent()

    val READER_MODE_SCRIPT = """
        (function() {
            var styleId = 'nora-injected-reader-mode';
            var existing = document.getElementById(styleId);
            if (existing) {
                existing.remove();
                return 'disabled';
            } else {
                var style = document.createElement('style');
                style.id = styleId;
                style.textContent = `
                    body {
                        max-width: 760px !important;
                        margin: 0 auto !important;
                        padding: 24px 16px !important;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Georgia, serif !important;
                        font-size: 19px !important;
                        line-height: 1.8 !important;
                        color: #2b2b2b !important;
                        background: #faf8f5 !important;
                    }
                    nav, header, footer, aside, .sidebar, .comments, .ad, .ads, .advertisement, [class*="popup"], [id*="cookie"] {
                        display: none !important;
                    }
                `;
                document.head.appendChild(style);
                return 'enabled';
            }
        })();
    """.trimIndent()

    val UNLOCK_COPY_SCRIPT = """
        (function() {
            var style = document.createElement('style');
            style.textContent = '* { -webkit-user-select: text !important; user-select: text !important; }';
            document.head.appendChild(style);
            
            ['contextmenu', 'selectstart', 'dragstart', 'copy', 'cut'].forEach(function(evt) {
                document.addEventListener(evt, function(e) { e.stopPropagation(); }, true);
                window.addEventListener(evt, function(e) { e.stopPropagation(); }, true);
            });
            document.oncontextmenu = null;
            document.onselectstart = null;
            window.oncontextmenu = null;
            return 'unlocked';
        })();
    """.trimIndent()

    val GET_HTML_SOURCE_SCRIPT = """
        (function() {
            return document.documentElement.outerHTML;
        })();
    """.trimIndent()
}
