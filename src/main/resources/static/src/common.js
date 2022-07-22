/**
 * Standard jQuery initialisation of the page were all buttons are assigned an
 * operation and the form doesn't really do anything.
 */
$(() => {
    console.log("Content Loaded");
});

/**
 * A helper function sets a value to null if it's empty/undefined.
 *
 * @param {*} obj   The object to be checked whether empty
 */
function nullIfEmpty(obj) {
    if (obj && obj != "null" && obj != "undefined") {
        return obj;
    }
    return null;
}

/**
 * A helper function that shows the error boostrap error dialog and displays
 * the provided error message in it.
 */
function showError(errorMsg) {
    $('#error-dialog').modal('show');
    $('#error-dialog .modal-body').html(`<p class="text-danger">${errorMsg}</p>`);
}