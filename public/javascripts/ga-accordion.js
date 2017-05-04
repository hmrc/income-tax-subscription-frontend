var open = false;

function markAccordionOpen(element) {
    if (!open) {
        open = true;
        console.log("Logged accordion open");
        ga('send', 'event', 'test', 'test', 'test')
    }
}
