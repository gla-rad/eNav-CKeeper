/**
 * Global variables
 */
var mrnEntitiesTable = undefined;

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
}, {
    data: "mrn",
    title: "MRN",
    hoverMsg: "MRN of the entity",
    placeholder: "MRN of the entity",
    required: true,
}, {
    data: "certificate",
    title: "Certificate",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "publicKey",
    title: "Public Key",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "privateKey",
    title: "Private Key",
    type: "hidden",
    visible: false,
    searchable: false
}];

// Run when the document is ready
$(document).ready( function () {
    mrnEntitiesTable = $('#mrn_entities_table').DataTable({
        //"processing": true,
        //"language": {
        //    processing: '<i class="fa fa-spinner fa-spin fa-3x fa-fw"></i><span class="sr-only">Loading...</span>',
        //},
        "serverSide": true,
        ajax: {
            "type": "POST",
            "url": "/api/mrn-entities/dt",
            "contentType": "application/json",
            "data": function (d) {
                return JSON.stringify(d);
            },
            error: function (jqXHR, ajaxOptions, thrownError) {
                console.error(thrownError);
            }
        },
        columns: mrnEntitiesColumnDefs,
        dom: 'Bfrltip',
        select: 'single',
        lengthMenu: [10, 25, 50, 75, 100],
        responsive: true,
        altEditor: true, // Enable altEditor
        buttons: [{
            text: '<i class="fas fa-plus-circle"></i>',
            titleAttr: 'Add Entity',
            name: 'add' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-edit"></i>',
            titleAttr: 'Edit Entity',
            name: 'edit' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-trash-alt"></i>',
            titleAttr: 'Delete Entity',
            name: 'delete' // do not change name
        }, {
           extend: 'selected', // Bind to Selected row
           text: '<i class="fas fa-cogs"></i>',
           titleAttr: 'Generate Certificate',
           name: 'generateCertificate', // do not change name
           className: 'generate-certificate-toggle',
           action: (e, dt, node, config) => {
               console.log("Generating MRN Entity Certificate")
           }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fab fa-cloudversify"></i>',
            titleAttr: 'Update MCP MIR',
            name: 'updateMIR', // do not change name
            className: 'update-mir-toggle',
            action: (e, dt, node, config) => {
                console.log("Registering the Certificate to the MCP MIR")
            }
        }],
        onAddRow: function (datatable, rowdata, success, error) {
            $.ajax({
                url: '/api/mrn-entities',
                type: 'POST',
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify({
                    id: rowdata["id"],
                    name: rowdata["name"],
                    mrn: rowdata["mrn"],
                    certificate: rowdata["certificate"],
                    publicKey: rowdata["publicKey"],
                    privateKey: rowdata["privateKey"]
                }),
                success: success,
                error: error
            });
        },
        onDeleteRow: function (datatable, rowdata, success, error) {
            $.ajax({
                url: `/api/mrn-entities/${rowdata["id"]}`,
                type: 'DELETE',
                success: success,
                error: error
            });
        },
        onEditRow: function (datatable, rowdata, success, error) {
            // The geometry is not read correctly so we need to access it in-direclty
            var idx = stationsTable.cell('.selected', 0).index();
            var data = stationsTable.rows(idx.row).data();
            var geometry = data[0].geometry;
            $.ajax({
                url: `/api/mrn-entities/${rowdata["id"]}`,
                type: 'PUT',
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify({
                    id: rowdata["id"],
                    name: rowdata["name"],
                    mrn: rowdata["mrn"],
                    certificate: rowdata["certificate"],
                    publicKey: rowdata["publicKey"],
                    privateKey: rowdata["privateKey"]
                }),
                success: success,
                error: error
            });
        }
    });
});
