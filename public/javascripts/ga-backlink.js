var open = false;

function backLinkClicked(href) {
    if (!open) {
        open = true;
        ga('send', 'event', 'itsa', 'back link clicked', href);
    }
}
