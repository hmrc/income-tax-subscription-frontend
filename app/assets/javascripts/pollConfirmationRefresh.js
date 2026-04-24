(function () {
    let script = document.currentScript;
    if (!script) return;

    let url = script.getAttribute("data-url");
    if (!url) return;

    const intervalMs = Number(script.getAttribute("data-interval"));

    async function check() {
        try {
            let res = await fetch(url, { cache: "no-store" });
            if (res.status !== 204) {
                location.reload();
                return;
            }
        } catch {}

        setTimeout(check, intervalMs);
    }

    check();
})();
