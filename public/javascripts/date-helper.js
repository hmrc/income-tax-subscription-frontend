$(document).ready(function () {
    var $day = $('input[name$="dateDay"]'),
        $month = $('input[name$="dateMonth"]'),
        $year = $('input[name$="dateYear"]');
    var BACKSPACE = 8;
    var TAB = 9;
    var SHIFT = 16;
    var LEFT_ARROW = 37;
    var RIGHT_ARROW = 39;
    var DELETE = 46;
    var ZERO = 48;
    var ZERO_NUMPAD = 96;
    var NINE = 57;
    var NINE_NUMPAD = 105;
    var whiteList = [BACKSPACE, DELETE, TAB, SHIFT, LEFT_ARROW, RIGHT_ARROW];

    function gotoNext(key, newValue, nextElement) {
        // delete & backspace are used for editing,
        // tab, shift + tab are combinations used for navigation
        // so the should not affect focus
        if (!whiteList.includes(key) && newValue.length == 2) {
            nextElement.focus();
            nextElement.select();
        }
    }

    function dayKeyUp(key, elem) {
        var name = elem.name;
        var newValue = $(elem).val();
        var month = name.replace("dateDay", "dateMonth");

        gotoNext(key, newValue, $(document.getElementById(month)));
    }

    function monthKeyUp(key, elem) {
        var name = elem.name;
        var newValue = $(elem).val();
        var year = name.replace("dateMonth", "dateYear");

        gotoNext(key, newValue, $(document.getElementById(year)));
    }

    function inputFilter(event) {
        var key = event.which;
        var modifier = event.shiftKey || event.ctrlKey || event.altKey || event.metaKey;
        if (whiteList.includes(key) ||
            ((event.metaKey || event.ctrlKey) && key == Z) || // undo/redo
            ((event.metaKey || event.ctrlKey) && key == Y) || // redo
            (!modifier && key >= ZERO && key <= NINE) || // only allows 0-9 but not when there's a modifier
            (!modifier && key >= ZERO_NUMPAD && key <= NINE_NUMPAD)) {
            return true
        }
        // prevent input from any other key
        event.preventDefault();
        return false
    }

    $day.on('keydown', function (e) {
        inputFilter(e);
    });

    $day.on('keyup', function (e) {
        var key = e.which;
        dayKeyUp(key, this);
    });

    $month.on('keydown', function (e) {
        inputFilter(e);
    });

    $month.on('keyup', function (e) {
        var key = e.which;
        monthKeyUp(key, this);
    });

    $year.on('keydown', function (e) {
        inputFilter(e);
    });


});
