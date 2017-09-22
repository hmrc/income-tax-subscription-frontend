/*! jQuery UI - v1.11.4 - 2015-03-11
 * http://jqueryui.com
 * Includes: core.js, widget.js, mouse.js, position.js, accordion.js, autocomplete.js, button.js, datepicker.js, dialog.js, draggable.js, droppable.js, effect.js, effect-blind.js, effect-bounce.js, effect-clip.js, effect-drop.js, effect-explode.js, effect-fade.js, effect-fold.js, effect-highlight.js, effect-puff.js, effect-pulsate.js, effect-scale.js, effect-shake.js, effect-size.js, effect-slide.js, effect-transfer.js, menu.js, progressbar.js, resizable.js, selectable.js, selectmenu.js, slider.js, sortable.js, spinner.js, tabs.js, tooltip.js
 * Copyright 2015 jQuery Foundation and other contributors; Licensed MIT */
$(document).ready($(function () {

    var cookieData = GOVUK.getCookie("mtd_ur_panel");
    if (cookieData == null) {
        $("#ur-panel").addClass("banner-panel--show");
    }

    $(".banner-panel__close").on("click", function (e) {
        e.preventDefault();
        GOVUK.setCookie("mtd_ur_panel", 1, 99999999999);
        $("#ur-panel").removeClass("banner-panel--show");
    });

}));