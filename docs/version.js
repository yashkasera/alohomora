(function () {
    const REPO = 'yashkasera/alohomora';
    const CACHE_KEY = 'alohomora_latest_version';
    const RELEASES_URL = 'https://github.com/' + REPO + '/releases';

    function applyVersion(version) {
        document.querySelectorAll('.alohomora-version').forEach(function (el) {
            el.textContent = el.textContent.replace('latest', version);
        });
        document.querySelectorAll('[data-version-template]').forEach(function (el) {
            el.textContent = el.getAttribute('data-version-template').replace(/{version}/g, version);
        });
        document.querySelectorAll('[data-copy-version]').forEach(function (btn) {
            var tpl = btn.getAttribute('data-copy-version');
            btn.setAttribute('onclick',
                "navigator.clipboard.writeText('" + tpl.replace(/{version}/g, version) + "');" +
                "this.textContent='Copied!';setTimeout(()=>{this.textContent='\\u2398'},1500)");
        });
    }

    function showFallbackLink() {
        document.querySelectorAll('.version-fallback').forEach(function (el) {
            el.style.display = 'inline';
        });
    }

    function init() {
        var cached = sessionStorage.getItem(CACHE_KEY);
        if (cached) {
            applyVersion(cached);
            return;
        }

        fetch('https://api.github.com/repos/' + REPO + '/releases/latest')
            .then(function (res) {
                if (!res.ok) throw new Error(res.status);
                return res.json();
            })
            .then(function (data) {
                var version = (data.tag_name || '').replace(/^v/, '');
                if (version) {
                    sessionStorage.setItem(CACHE_KEY, version);
                    applyVersion(version);
                } else {
                    showFallbackLink();
                }
            })
            .catch(function () {
                showFallbackLink();
            });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
