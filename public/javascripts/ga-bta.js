$(document).ready($(function () {

    var $btaLink = $("#bta");

    $btaLink.on('click', function (e) {
        if (typeof ga === "function") {
            e.preventDefault();
            ga('send', 'event', 'itsa', 'bta link clicked', 'value',{
                hitCallback: function () {
                    window.location = $btaLink.attr("href")
                }
            });
        }
    });

}));