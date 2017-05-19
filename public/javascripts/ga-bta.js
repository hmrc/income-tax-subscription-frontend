$(document).ready($(function () {

    var $btaLink = $("#bta");

    $btaLink.on('click', function (e) {
        e.preventDefault();
        if (typeof ga === "function" && $btaLink != undefined) {
            ga('send', 'event', 'itsa', 'bta link clicked', 'value',{
                hitCallback: function () {
                    window.location = $btaLink.attr("href")
                }
            });
        }else {
            window.location = $btaLink.attr("href")
        }
    });

}));