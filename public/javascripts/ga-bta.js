$(document).ready($(function () {

    var $btaLink = $("#bta");

    $btaLink.on('click', function (e) {
            ga('send', 'event', 'link clicked', 'bta link');
    });

}));