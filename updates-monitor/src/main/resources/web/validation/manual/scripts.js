var dataCursor = 0;
var data;

function init() {
    xhttp = new XMLHttpRequest();
    xhttp.open("GET", "/ajax_list", false);
    xhttp.send();
    data = JSON.parse(xhttp.responseText);

    fillView();
}

function fillView() {
    if (dataCursor == data.length) {
        done();
        return;
    }

    do {
        var dataRow = data[dataCursor];
        if (dataRow['processed']) {
            dataCursor++;
        }
    } while (dataRow['processed'] && dataCursor < data.length);

    if (dataCursor == data.length) {
        done();
        return;
    }

    document.getElementById('title').innerText = dataRow['videoId'] + " - " + dataRow['title'];
    document.getElementById('image').src = dataRow['filename'];
}

function done() {
    document.getElementById('title').innerText = 'No more data';
    document.getElementById('image').src = '';
}

function del() {
    var dataRow = data[dataCursor];
    xhttp = new XMLHttpRequest();
    xhttp.open('GET', '/ajax_del?id=' + dataRow['videoId'], true);
    xhttp.send();

    dataCursor++;
    fillView();
}

function keep() {
    var dataRow = data[dataCursor];
    xhttp = new XMLHttpRequest();
    xhttp.open('GET', '/ajax_keep?id=' + dataRow['videoId'], true);
    xhttp.send();

    dataCursor++;
    fillView();
}

function protect(button) {
    button.style.display = 'none';

    var promise = new Promise(function(resolve, reject) {
        setTimeout(() => button.style.display = 'inline', 1000);
    });
}
