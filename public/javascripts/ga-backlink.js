$(document).ready($(function () {

    var $backLink = $("#back");

    $backLink.on('click', function (e) {
        if (typeof ga === "function") {
            e.preventDefault();
            var backLinkClickedTitle = $('title').text();
            ga('send', 'event', 'itsa', 'back link clicked', backLinkClickedTitle, {
                hitCallback: function () {
                    window.location = $backLink.attr("href")
                }
            });
        }
    });

}));
