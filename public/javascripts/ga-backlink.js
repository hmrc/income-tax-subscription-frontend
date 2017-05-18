function backLinkClicked() {
    var backLinkClickedTitle = $('title').text();
    ga('send', 'event', 'itsa', 'back link clicked', backLinkClickedTitle);
}
