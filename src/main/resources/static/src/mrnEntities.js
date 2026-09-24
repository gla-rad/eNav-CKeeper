/**
 * Global variables
 */
var mrnEntitiesTable = undefined;
var certificatesTable = undefined;

/**
 * The MRN Entities Table Column Definitions
 * @type {Array}
 */
var mrnEntitiesColumnDefs = [{
    data: "id",
    title: "ID",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "name",
    title: "Name",
    hoverMsg: "Name of the entity",
    placeholder: "Name of the entity",
    required: true,
    render: (data, type) => type === 'display' ? `<span class="fw-semibold">${escapeHtml(data)}</span>` : data
}, {
    data: "mrn",
    title: "MRN",
    hoverMsg: "MRN of the entity",
    placeholder: "MRN of the entity",
    required: true,
    render: (data, type) => type === 'display' ? renderIdentifier(data) : data
}, {
    data: "mmsi",
    title: "MMSI",
    hoverMsg: "MMSI of the entity",
    placeholder: "MMSI of the entity",
    render: (data, type) => type === 'display' ? renderIdentifier(data) : data
}, {
    data: "entityType",
    title: "Entity Type",
    hoverMsg: "The type of the entity",
    placeholder: "The type of the entity",
    type: "select",
    options: {
        "device": "Device",
        "service": "Service",
        "vessel": "Vessel"
    },
    required: true,
    render: (data, type) => type === 'display' ? renderEntityType(data) : data
}, {
    data: "version",
    title: "Version (For Services)",
    hoverMsg: "The version of the entity",
    placeholder: "The version of the entity",
    required: false,
    render: (data, type) => type === 'display' ? renderTag(data) : data
 }];

/**
 * The MRN Entities Table Column Definitions
 * @type {Array}
 */
var certificatesColumnDefs = [{
    data: "id",
    title: "ID",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "mrnEntityId",
    title: "MRN Entity ID",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "publicKey",
    title: "Public Key",
    hoverMsg: "The Certificate Public Key",
    placeholder: "The Certificate Public Key",
    required: true,
    width: "40%",
    render: (data, type) => type === 'display' ? renderPublicKey(data) : data
}, {
    data: "startDate",
    title: "Start Date",
    type: "date",
    dateFormat: "MMM DD, YYYY",
    render: (data, type) => type === 'display' ? renderDate(data) : data,
    hoverMsg: "The Certificate is Valid From",
    placeholder: "The Certificate is Valid From",
    width: "25%"
}, {
    data: "endDate",
    title: "End Date",
    type: "date",
    dateFormat: "MMM DD, YYYY",
    render: (data, type) => type === 'display' ? renderDate(data) : data,
    hoverMsg: "The Certificate is Valid Until",
    placeholder: "The Certificate is Valid Until",
    width: "25%"
}, {
    data: "revoked",
    title: "Revoked",
    type: "select",
    options: {
        true: "true",
        false: "false"
    },
    render: (data, type) => type === 'display' ? renderRevoked(data) : data,
    className: "dt-body-center",
    hoverMsg: "Revoked",
    placeholder: "Revoked",
    width: "10%"
}];

/**
 * Renders the type of an MRN entity as a tag.
 *
 * @param {String}  entityType  The entity type
 * @return {String} The entity type markup
 */
function renderEntityType(entityType) {
    const icons = {
        device: 'fa-microchip',
        service: 'fa-server',
        vessel: 'fa-ship'
    };
    if (!entityType) {
        return renderTag(entityType);
    }
    const icon = icons[entityType] ? `<i class="fa-solid ${icons[entityType]}"></i>` : '';
    const label = entityType.charAt(0).toUpperCase() + entityType.slice(1);
    return `<span class="tag tag-accent">${icon}${escapeHtml(label)}</span>`;
}

/**
 * Renders a certificate public key inside a read-only monospaced text area,
 * so that the long PEM content stays contained within its cell.
 *
 * @param {String}  publicKey   The certificate public key
 * @return {String} The public key markup
 */
function renderPublicKey(publicKey) {
    const content = publicKey ? $('<div>').text(publicKey).html() : '';
    return `<textarea class="form-control form-control-sm code-view" rows="4" readonly>${content}</textarea>`;
}

/**
 * Renders a certificate validity date in a compact representation.
 *
 * @param {String}  value   The certificate date
 * @return {String} The date markup
 */
function renderDate(value) {
    return value
        ? `<span class="cell-mono">${escapeHtml(moment(value).format("MMM DD, YYYY"))}</span>`
        : '<span class="cell-muted">&mdash;</span>';
}

/**
 * Renders the revocation status of a certificate.
 *
 * @param {Boolean} revoked     Whether the certificate has been revoked
 * @return {String} The status markup
 */
function renderRevoked(revoked) {
    return (revoked === true || revoked === "true")
        ? '<span class="tag tag-danger"><i class="fa-solid fa-ban"></i>Revoked</span>'
        : '<span class="tag tag-success"><i class="fa-solid fa-circle-check"></i>Valid</span>';
}

// Run when the document is ready
$(() => {
    mrnEntitiesTable = $('#mrn_entities_table').DataTable($.extend(commonDatatableOptions(), {
        ajax: {
            "type": "POST",
            "url": "./api/mrn-entity/dt",
            "contentType": "application/json",
            "data": (d) => {
                return JSON.stringify(d);
            },
            error: (response, status, more) => {
                showErrorDialog(extractErrorMessage(response));
            }
        },
        columns: mrnEntitiesColumnDefs,
        altEditor: true, // Enable altEditor
        buttons: [{
            text: '<i class="fa-solid fa-circle-plus"></i><span class="dt-button-text">New</span>',
            titleAttr: 'Add a new entity',
            name: 'add' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-pen-to-square"></i><span class="dt-button-text">Edit</span>',
            titleAttr: 'Edit the selected entity',
            name: 'edit' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-certificate"></i><span class="dt-button-text">Certificates</span>',
            titleAttr: 'View the certificates of the selected entity',
            name: 'certificates', // do not change name
            className: 'certificates-toggle',
            action: (e, dt, node, config) => {
                loadMrnEntityCertificates(e, dt, node, config)
            }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-trash-can"></i><span class="dt-button-text">Delete</span>',
            titleAttr: 'Delete the selected entity',
            className: 'btn-danger-soft',
            name: 'delete' // do not change name
        }].concat(commonDatatableButtons('MRN Entities')),
        onAddRow: (datatable, rowdata, success, error) => {
            $.ajax({
                url: './api/mrn-entity',
                type: 'POST',
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify({
                    id: null,
                    name: nullIfEmpty(rowdata["name"]),
                    mrn: nullIfEmpty(rowdata["mrn"]),
                    mmsi: nullIfEmpty(rowdata["mmsi"]),
                    entityType: nullIfEmpty(rowdata["entityType"]),
                    version: nullIfEmpty(rowdata["version"])
                }),
                success: (response) => {
                    showToast('The entity has been saved', 'success');
                    success(response);
                },
                error: (response, status, more) => {
                    error({"responseText" : extractErrorMessage(response)}, status, more);
                }
            });
        },
        onEditRow: (datatable, rowdata, success, error) => {
            $.ajax({
                type: 'PUT',
                url: `./api/mrn-entity/${rowdata["id"]}`,
                crossDomain: true,
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify({
                    id: nullIfEmpty(rowdata["id"]),
                    name: nullIfEmpty(rowdata["name"]),
                    mrn: nullIfEmpty(rowdata["mrn"]),
                    mmsi: nullIfEmpty(rowdata["mmsi"]),
                    entityType: nullIfEmpty(rowdata["entityType"]),
                    version: nullIfEmpty(rowdata["version"])
                }),
                success: (response) => {
                    showToast('The entity has been saved', 'success');
                    success(response);
                },
                error: (response, status, more) => {
                    error({"responseText" : extractErrorMessage(response)}, status, more);
                }
            });
        },
        onDeleteRow: (datatable, selectedRows, success, error) => {
             selectedRows.every(function(rowIdx, tableLoop, rowLoop) {
                 $.ajax({
                     url: `./api/mrn-entity/${this.data()["id"]}`,
                     type: 'DELETE',
                     success: success,
                     error: (response, status, more) => {
                         error({"responseText" : extractErrorMessage(response)}, status, more);
                     }
                 });
             });
        }
    }));

    // We also need to link the certificates toggle button with the the modal
    // panel so that by clicking the button the panel pops up. It's easier done
    // with jQuery.
    mrnEntitiesTable.buttons('.certificates-toggle')
        .nodes()
        .attr({ "data-bs-toggle": "modal", "data-bs-target": "#certificatesPanel" });

    // On confirmation of the certificate generation, we need to make an AJAX
    // call back to the service to generate a new certificate
    $('#confirm-generate-certificate').on('click', '.btn-ok', (e) => {
        var $modalDiv = $(e.delegateTarget);
        var idx = mrnEntitiesTable.cell('.selected', 0).index();
        var data = mrnEntitiesTable.rows(idx.row).data();
        var mrnEntityId = data[0].id;
        var successMessage = 'A new certificate has been generated';
        $modalDiv.addClass('loading');
        $.ajax({
            url: `./api/mrn-entity/${mrnEntityId}/certificates`,
            type: 'PUT',
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: () => {
                // Wait for a sec or so... and reload
                setTimeout(() => {
                    certificatesTable.ajax.reload();
                    $modalDiv.modal('hide').removeClass('loading');
                    showToast(successMessage, 'success');
                }, 10000);
            },
            error: (response, status, more)  => {
                $modalDiv.removeClass('loading');
                showErrorDialog(extractErrorMessage(response));
            }
        });
    });

    // On confirmation of the certificate revoke, we need to make an AJAX
    // call back to the service to revoke the selected certificate
    $('#confirm-revoke-certificate').on('click', '.btn-ok', (e) => {
        var $modalDiv = $(e.delegateTarget);
        var idx = certificatesTable.cell('.selected', 0).index();
        var data = certificatesTable.rows(idx.row).data();
        var certificateId = data[0].id;
        var successMessage = 'The certificate has been revoked';
        $modalDiv.addClass('loading');
        $.ajax({
            url: `./api/certificate/${certificateId}/revoke`,
            type: 'PUT',
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: () => {
                // Wait for a sec or so... and reload
                setTimeout(() => {
                    certificatesTable.ajax.reload();
                    $modalDiv.modal('hide').removeClass('loading');
                    showToast(successMessage, 'success');
                }, 10000);
            },
            error: (response, status, more) => {
                $modalDiv.removeClass('loading');
                showErrorDialog(extractErrorMessage(response));
            }
        });
    });
});

/**
 * This function will initialise the certificated_table DOM element and loads
 * the certificates applicable for the provided row's MRN entity ID.
 *
 * @param {Event}         event         The event that took place
 * @param {DataTable}     table         The MRN entities table
 * @param {Node}          button        The button node that was pressed
 * @param {Configuration} config        The table configuration
 */
function loadMrnEntityCertificates(event, table, button, config) {
    var idx = table.cell('.selected', 0).index();
    var data = table.rows(idx.row).data();
    var mrnEntityId = data[0].id;
    var mrnEntityType = data[0].entityType;

    // Destroy the table if it already exists
    if (certificatesTable) {
        certificatesTable.destroy();
        certificatesTable = undefined;
    }

    // And re-initialise it
    certificatesTable = $('#certificates_table').DataTable($.extend(commonDatatableOptions(), {
        // The certificates are all loaded at once and handled client side
        serverSide: false,
        stateSave: false,
        pageLength: 10,
        ajax: {
            "type": "GET",
            "url": `./api/mrn-entity/${mrnEntityId}/certificates`,
            "dataType": "json",
            "cache": false,
            "dataSrc": "",
            error: (response, status, more) => {
                showErrorDialog(extractErrorMessage(response));
            }
        },
        columns: certificatesColumnDefs,
        altEditor: true, // Enable altEditor
        buttons: [{
            text: '<i class="fa-solid fa-gears"></i><span class="dt-button-text">Generate</span>',
            titleAttr: 'Generate a new certificate',
            name: 'generateCertificate', // do not change name
            action: (e, dt, node, config) => {
                $('#confirm-generate-certificate').modal('show');
            }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-ban"></i><span class="dt-button-text">Revoke</span>',
            titleAttr: 'Revoke the selected certificate',
            name: 'revokeCertificate', // do not change name
            className: 'btn-danger-soft',
            action: (e, dt, node, config) => {
                $('#confirm-revoke-certificate').modal('show');
            }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-trash-can"></i><span class="dt-button-text">Delete</span>',
            titleAttr: 'Delete the selected certificate',
            className: 'btn-danger-soft',
            name: 'delete' // do not change name
        }].concat(commonDatatableButtons('Entity Certificates')),
        onDeleteRow: (datatable, selectedRows, success, error) => {
            selectedRows.every(function(rowIdx, tableLoop, rowLoop) {
                if(!this.data()["revoked"] || this.data()["revoked"]=="false") {
                    $(datatable.modal_selector).modal('hide');
                    $('.reveal-overlay').hide();
                    showErrorDialog("You can only delete a certificate if it has first been revoked.\nPlease revoke it and try again...");
                } else {
                    $.ajax({
                        url: `./api/certificate/${this.data()["id"]}`,
                        type: 'DELETE',
                        crossDomain: true,
                        success: () => {
                            $(datatable.modal_selector).modal('hide');
                            $('.reveal-overlay').hide();
                            certificatesTable.ajax.reload();
                            showToast('The certificate has been deleted', 'success');
                        },
                        error: (response, status, more) => {
                            $(datatable.modal_selector).modal('hide');
                            $('.reveal-overlay').hide();
                            error({"responseText" : extractErrorMessage(response)}, status, more);
                        }
                    });
                }
            })
        }
    }));
}
