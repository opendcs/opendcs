class OpenDcsDataTable {
  constructor(domId, properties, inlineOptions, actions, initialize) {
	  console.log("In super constructor for OpenDcsDataTable.");
	  //Remove # if the passed id starts with one.
	  if (domId.startsWith("#"))
	  {
		  domId = domId.slice(1);
	  }
    this.domId = domId;
    this.jq = $(`#${domId}`);
    
    this.properties = properties;
    
    this.setInlineOptions(inlineOptions);
    
    this.actions = actions;
    
    this.dataTable = null;
    
    if (initialize)
	{
    	this.initDataTable();
	}
  }
  
  initDataTable() {
    this.dataTable = this.jq.DataTable(this.properties);
    this.jq.closest(".dataTables_wrapper").find("[function=addBlankRow]")
    	.on("click", {thisObject: this}, function(e) {
            e.data.thisObject.addBlankRowToDataTable(true);
    	});	
  }
  
  setInlineOptions(inlineOptions)
  {
	  this.inlineOptions = inlineOptions;
  }
  
  setDataTable() {
	  this.jq.DataTable(this.properties);
  }
	
  addRow(newRow) {
	  this.dataTable.row.add(newRow);
  }
  
  clearDataTable(paging) {
	  if (paging == null)
	  {
		  paging = false;
	  }
	  this.dataTable.init();
	  this.dataTable.clear();
	  this.dataTable.draw(paging);
  }
  createActionDropdown(applicableActions)
  {
      var actionRefs = {
              "delete": {
                  "target": "<!-- DELETE_HTML -->",
                  "html": '<a <!-- ONCLICK --> class="dropdown-item" data-ignorefocusout="true" action_type="delete_row"><i class="icon-cancel-circle2"></i> Delete</a>',
                  "default_onclick": "deleteRow_default(event, this)"
              },
              "copy": {
                  "target": "<!-- COPY_HTML -->",
                  "html": '<a <!-- ONCLICK --> class="dropdown-item" data-ignorefocusout="true" action_type="copy_row"><i class="icon-cancel-circle2"></i> Copy</a>',
                  "default_onclick": "copyRow_default(event, this)"
              }
      };

      var actionDropdownHtml = '<div class="list-icons float-right">'
          + '<div class="dropdown">'
          + '    <a href="javascript:void(0)" class="list-icons-item" data-toggle="dropdown" onclick="clickedDropdown(event, this)" data-ignorefocusout="true">'
          + '    <i class="icon-menu9"></i>'
          + '</a>'
          + '<div class="dropdown-menu dropdown-menu-right">'
          + "<!-- COPY_HTML -->"
          + "<!-- DELETE_HTML -->"
          + '</div>'
          + '</div>'
          + '</div>';

      applicableActions.forEach(applicAction => {

          var type = applicAction["type"];
          var onclick = applicAction["onclick"] != null ? applicAction["onclick"] : actionRefs[type]["default_onclick"];
          actionDropdownHtml = actionDropdownHtml.replace(actionRefs[type]["target"], actionRefs[type]["html"].replace("<!-- ONCLICK -->", `onclick='${onclick}'`));
      });

      return actionDropdownHtml;
  }
  
  addBlankRowToDataTable(redraw, dragIcon)
  {
	  console.log("Adding blank row to datatable.");
      var numCols = this.dataTable.columns().nodes().length;
      var emptyRow = new Array(numCols);
      emptyRow.fill("");
      if (this.actions != null)
      {
          emptyRow[numCols-1] = createActionDropdown(this.actions);
      }
      if (dragIcon != null)
      {
          emptyRow[0] = '<i class="move-cursor icon-arrow-resize8 mr-3 icon-1x"></i>';
      }
      this.addRow(emptyRow);
      if (redraw)
      {
    	  this.dataTable.draw();
      }
      if (this.inlineOptions != null)
      {
          this.makeTableInline();
      }
  }
  
  getNonDeletedRowData()
  {
      var data = this.dataTable.data();
      var returnRows = [];
      for (var x = 0; x < data.length; x++)
      {
          var deleteAttr = $(this.dataTable.row(x).node()).attr("delete");
          if (deleteAttr == null || deleteAttr.toLowerCase() != "true")
          {
              returnRows.push(data[x]);
          }
      }
      return returnRows;
  }
  

  getDataTableScrollHeight(spacingBottomPercentage)
  {
      var cardHeight = this.jq.closest(".card").height();
      var spacingBottom = cardHeight * spacingBottomPercentage/100;
      var wrapper = this.jq.closest("#" + this.jq.attr("id") + "_wrapper");
      var tableHeight = this.jq.height();
      var tableHeaderHeight = wrapper.find(".dataTables_scrollHead").height();
      var tableButtonsHeight = wrapper.find(".dt-buttons").length > 0 ? wrapper.find(".dt-buttons").height() : 0;
      
      var scrollHeight = cardHeight - tableHeaderHeight - tableButtonsHeight - spacingBottom;
      return scrollHeight;
  }

  getDatatableObjects()
  {
      var jqTable = this.jq;
      var dt = this.dataTable;
      dt.draw();

      var wrapper = jqTable.closest(".dataTables_wrapper");

      var parentDiv = wrapper.parent();
      var buttons = wrapper.find(".dt-buttons");
      var header = wrapper.find(".dataTables_scrollHead");
      var body = wrapper.find(".dataTables_scrollBody");
      var footer = wrapper.find(".dataTables_scrollFoot");

      var objects = {
              "parent_div": parentDiv,
              "wrapper": wrapper,
              "buttons": buttons,
              "header": header,
              "body": body,
              "footer": footer
      }
      return objects;
  }
  
  updateDataTableScroll(fillPercentage)
  {
      var jqTable = this.jq;
      var dt = this.dataTable;
      dt.draw();

      var objs = this.getDatatableObjects();
      var totalHeight = objs["parent_div"].height();

      if (fillPercentage == null)
      {
          var resizeVal = jqTable.attr("resize_on_window_resize");
          try {
              resizeVal = parseFloat(resizeVal)
          }
          catch (error) {
              //Error parsing float ${resizeVal}.
              resizeVal = 100;
          }
          fillPercentage = resizeVal;
      }
      totalHeight = totalHeight * (fillPercentage / 100.0);
      var buttonHeight = 0;
      if (objs.buttons.length > 0)
      {
          buttonHeight = objs.buttons.outerHeight(includeMargin=true);
      }
      var bodyHeight = totalHeight - objs.header.height() - buttonHeight - objs.footer.height();

      objs.body.css("max-height", bodyHeight);
      objs.body.css("height", bodyHeight);

      dt.draw();
  }
  
  makeTableInline(runAtEndClick, runAtEndFocusOut)
  {
      var jqTableId = `#${this.domId}`;
      $(jqTableId + ' tbody tr td').off('click');
      $(jqTableId + ' tbody tr td').on('click', {thisObject: this}, function(e) {
          //These rows are not editable.
          var clickedWidth = $(this).width();

          var clickable = $(this).attr("clickable");
          if (clickable != null && clickable.toLowerCase() == "false")
          {
              //Cell not clickable, returning early.
              return;
          }
          var targetTable = $(jqTableId).DataTable();
          var clickedCell = targetTable.cell(this);
          if (clickedCell.length > 0)
          {
              var colNumOld = clickedCell[0][0].column;
              var rowNum = clickedCell[0][0].row;
              
              var dtColNum = targetTable.column(this).index(); //This is the col num including hidden columns.
              var colNum = getRealColumnIndex(this); //This is to take into account invisible columns;
              
              if (!$(jqTableId).hasClass("editing")) {
                  var data = targetTable.row(rowNum).data()
                  var $row = $(this.parentElement);
                  var thisPosition = $row.find("td:nth-child(" + (colNum + 1).toString() + ")");
                  var thisPositionText = thisPosition.text();


                  var inputId = "Position_" + rowNum.toString() + "_" + colNum.toString();
                  var inputId2 = "Position2_" + rowNum.toString() + "_" + colNum.toString();
                  var inputId3 = "Position3_" + rowNum.toString() + "_" + colNum.toString();

                  var forcedParam = $(this).attr("forced_propspec_type");
                  if (forcedParam != null)
                  {
                      $(jqTableId).addClass("editing");
                      var splitForcedParam = forcedParam.split(":");
                      if (splitForcedParam.length > 1)
                      {
                          forcedParam = splitForcedParam[0];
                      }
                      if (forcedParam == "b")
                      {
                          var curVal = this.textContent.toLowerCase();
                          var selectHtml = `<select style="width:${clickedWidth}" id="${inputId}">`;
                          ["", "true", "false"].forEach(d => {
                              var selectedFlag = "";
                              if (curVal == d.toLowerCase())
                              {
                                  selectedFlag = 'selected';
                              }
                              selectHtml += `<option value="${d}" ${selectedFlag}>${d}</option>`;
                          })
                          selectHtml += "</select>";
                          thisPosition.empty().append($(selectHtml));
                      }
                      else if (forcedParam == "e" && splitForcedParam.length > 1)
                      {
                          var curVal = this.textContent.toLowerCase();
                          var selectType = splitForcedParam[1].toLowerCase();
                          if (selectType == "season")
                          {
                              var sn = propSpecsMeta["seasons"].map(s => s.abbr);
                              var selectHtml = `<select id="${inputId}"><option></option>`;
                              sn.forEach(s => {
                                  var selectedFlag = "";
                                  if (curVal == s.toLowerCase())
                                  {
                                      selectedFlag = 'selected';
                                  }
                                  selectHtml += `<option value="${s}" ${selectedFlag}>${s}</option>`;
                              })
                              selectHtml += "</select>";
                              thisPosition.empty().append($(selectHtml));
                          }
                          else if (selectType == "transportmediumtype")
                          {
                              var curVal = this.textContent.toLowerCase();
                              var tmTypes = Object.keys(propSpecsMeta["transportMediumTypes"]);
                              var selectHtml = `<select id="${inputId}"><option></option>`;
                              tmTypes.forEach(tmt => {
                                  var selectedFlag = "";
                                  if (curVal == tmt.toLowerCase())
                                  {
                                      selectedFlag = 'selected';
                                  }
                                  selectHtml += `<option value="${tmt}" ${selectedFlag}>${tmt}</option>`;
                              })
                              selectHtml += "</select>";
                              thisPosition.empty().append($(selectHtml));
                          }

                      }
                      else if (forcedParam == "i")
                      {
                          thisPosition.empty().append($("<input></input>", {
                              "id": inputId,
                              "type": "number"
                          }));
                          $("#" + inputId).val(thisPositionText);
                      }
                      else if (forcedParam == "s" || forcedParam == "h" || forcedParam == "n" || forcedParam == "f" || forcedParam == "f")
                      {
                          thisPosition.empty().append($("<input></input>", {
                              "id": inputId
                          }));
                          $("#" + inputId).val(thisPositionText);
                      }
                  }
                  else
                  {
                      for (var x = 0; x < e.data.thisObject.inlineOptions.columnDefs.length; x++)
                      {
                          var colOptions = e.data.thisObject.inlineOptions.columnDefs[x];
                          if (colOptions.targets.indexOf(dtColNum) != -1)
                          {
                              $(jqTableId).addClass("editing");
                              if (colOptions.type == "input")
                              {
                                  thisPosition.empty().append($("<input></input>", {
                                      "id": inputId
                                  }));
                                  $("#" + inputId).val(thisPositionText);
                                  break;
                              }
                              else if (colOptions.type == "number")
                              {
                                  thisPosition.empty().append($("<input></input>", {
                                      "id": inputId,
                                      "type": "number"
                                  }));
                                  $("#" + inputId).val(thisPositionText);
                                  break;
                              }
                              else if (colOptions.type == "textarea")
                              {
                                  thisPosition.empty().append($('<div class="form-group">' +
                                          '<textarea class="form-control" id="' + inputId + '"></textarea>' +
                                  '</div>'));
                                  $("#" + inputId).val(thisPositionText);
                                  break;
                              }
                              else if (colOptions.type == "select")
                              {
                                  var curVal = this.textContent.toLowerCase();
                                  var selectHtml = `<select id="${inputId}">`;
                                  colOptions.data.forEach(d => {
                                      var selectedFlag = "";
                                      if (curVal == d.toLowerCase())
                                      {
                                          selectedFlag = 'selected';
                                      }
                                      selectHtml += `<option value="${d}" ${selectedFlag}>${d}</option>`;
                                  })
                                  selectHtml += "</select>";

                                  //TODO: Need to sanitize for code injection.
                                  thisPosition.empty().append($(selectHtml));
                                  break;
                              }
                              else if (colOptions.type == "searchable_select")
                              {
                                  var optionGroupHtml = "";
                                  for (var x = 0; x < colOptions.data.length; x++)
                                  {
                                      var fullName = colOptions.data[x];
                                      var splitName = fullName.split("-");
                                      var baseName = `${splitName[0]}-*`;
                                      var subName = splitName.length > 1 ? `*-${splitName[1]}` : null;
                                      var optionHtml = `<optgroup label="${fullName}">
                                          <option value="${fullName}">${fullName}</option>
                                          <option value="${baseName}">${baseName}</option>`
                                          optionHtml += subName != null ? `<option value=${subName}>${subName}</option>` : "";
                                      optionHtml += `</optgroup>`;
                                      optionGroupHtml += optionHtml;
                                  }

                                  var objHtml = `
                                      <div data-type="special_select" id="${inputId}">
                                      <select class="form-control select-search" data-fouc>

                                      ${optionGroupHtml}

                                      </select>
                                      </div>`;
                                  thisPosition.empty().append($(objHtml));
                                  $(`#${inputId}`).find("select").select2({
                                      matcher(params, data) {
                                          const originalMatcher = $.fn.select2.defaults.defaults.matcher;
                                          const result = originalMatcher(params, data);

                                          if (
                                                  result &&
                                                  data.children &&
                                                  result.children &&
                                                  data.children.length
                                          ) {
                                              if (
                                                      data.children.length !== result.children.length &&
                                                      data.text.toLowerCase().includes(params.term.toLowerCase())
                                              ) {
                                                  result.children = data.children;
                                              }
                                              return result;
                                          }

                                          return null;
                                      },
                                  });
                                  $(`#${inputId}`).find("select").val(thisPositionText).trigger("change");
                                  $(`#${inputId}`).closest("div").children().on("mousedown", function(e) {
                                      var select = $(this).closest("div");
                                      event.preventDefault();
                                      var curTs = new Date();
                                      var tsString = curTs.toISOString();
                                      select.get(0).dataset["time_clicked"] = tsString;
                                  });
                                  break;
                              }
                              else if (colOptions.type == "editable_select")
                              {
                                  var curVal = this.textContent.toLowerCase();
                                  var selectHtml = `<div data-type="special_select" id="${inputId}" class="select-editable"><select id="${inputId3}" onchange="this.nextElementSibling.value=this.value">`;
                                  colOptions.data.forEach(d => {
                                      var selectedFlag = "";
                                      if (curVal == d.toLowerCase())
                                      {
                                          selectedFlag = 'selected';
                                      }
                                      selectHtml += `<option value="${d}" ${selectedFlag}>${d}</option>`;
                                  })
                                  selectHtml += `</select>
                                      <input id="${inputId2}" type="text" name="format" value="" />
                                      </div>`;
                                  thisPosition.empty().append($(selectHtml));



                                  var actualInput = $("#" + inputId2);
                                  $(thisPosition.find("select")).trigger("change");
                                  $(thisPosition.find("select")).focus();

                                  var selectInput = $("#" + inputId3);

                                  actualInput.val(thisPositionText);
                                  actualInput.on("mousedown", function(e) {
                                      var curTs = new Date();
                                      var tsString = curTs.toISOString();
                                      $(this).closest("div")[0].dataset["time_clicked"] = tsString;
                                  });
                                  selectInput.on("mousedown", function(e) {
                                      var curTs = new Date();
                                      var tsString = curTs.toISOString();
                                      $(this).closest("div")[0].dataset["time_clicked"] = tsString;
                                  });
                                  break;
                              }
                              else
                              {
                                  $('#' + tableId).removeClass("editing");
                                  break;
                              }
                          }
                      }
                  }

                  if (colOptions != null && colOptions.type != "searchable_select")
                  {
                      $("#" + inputId).focus();
                      $("#" + inputId).find("select").focus(); //This is to focus on the editable_select option.
                      $("#" + inputId).select();
                      $("#" + inputId).keypress(function(event) {
                          var keycode = (event.keyCode ? event.keyCode : event.which);
                          if(keycode == '13'){
                              this.blur();
                              $("#" + this.id).trigger("focusout");
                          }
                      });
                  }
                  $("#" + inputId).data("prev_val", thisPositionText);


              }

          }

          if (runAtEndClick != null)
          {
              runAtEndClick(this);
          }
      });
      
      $(jqTableId + ' tbody tr td').off('focusout');
      $(jqTableId + ' tbody tr td').on("focusout", {thisObject: this}, function(e) {
          var targetTable = $(jqTableId).DataTable();
          var visibleColumns = targetTable.columns().visible();
          var clickedCell = targetTable.cell(this);
          var ignoreFocusOut = event.path[0].dataset["ignorefocusout"];

          var editableSelectDiv = $(this).find("[data-type=special_select]");
          var editableInput = null;
          var keepFocus = false;
          if (editableSelectDiv.length > 0)
          {
              var timeClickedString = editableSelectDiv.data("time_clicked");
              if (timeClickedString != null)
              {
                  var timeClicked = new Date(timeClickedString);
                  var curDateTs = new Date();
                  //If you this check is performed within 0.140 seconds of the 
                  //click on the text box, it will keep the box open.
                  if ((curDateTs - timeClicked) <= 250) 
                  {
                      keepFocus = true;
                  }
              }
          }
          if ((ignoreFocusOut != null && ignoreFocusOut.toLowerCase() == "true") || keepFocus)
          {
              return
          }
          if (clickedCell.length > 0)
          {
              var colNumOld = clickedCell[0][0].column;
              var rowNum = clickedCell[0][0].row;
              
              var dtColNum = targetTable.column(this).index(); //This is the col num including hidden columns.
              var colNum = getRealColumnIndex(this); //This is to take into account invisible columns;
              
              var inputId = "Position_" + rowNum.toString() + "_" + colNum.toString();
              var thisVal = $("#" + inputId).val();
              if (editableSelectDiv.length > 0)
              {
                  var selectSearchVal = editableSelectDiv.find(".select-search").val();
                  var editSearchVal = editableSelectDiv.find("input").val();
                  thisVal = selectSearchVal != null ? selectSearchVal : editSearchVal; 
              }
              var previousVal = $("#" + inputId).data("prev_val");
              var $this = $(this);
              var $thisCell = targetTable.cell($this);
              var tempData = targetTable.row($this.closest("tr")).data().slice();
              tempData[dtColNum] = thisVal;
              targetTable.row($this.closest("tr")).data(tempData);
              /**
               * insted of updateing dom via jQuery use DataTable's Method to 
               * update dom object and 
               * also not forget to draw() your changes.
               *
               * $this.parent("td").empty().text($this.val());
               *
               **/
              $thisCell.data(thisVal).draw();
              for (var x = 0; x < e.data.thisObject.inlineOptions.columnDefs.length; x++)
              {
                  var def = e.data.thisObject.inlineOptions.columnDefs[x];
                  if (def.targets.indexOf(dtColNum) != -1)
                  {
                      if (previousVal != thisVal)
                      {
                          if ("bgcolor" in def && "change" in def["bgcolor"] && def["bgcolor"]["change"] != null)
                          {
                              var bgColor = def["bgcolor"]["change"];
                              $thisCell.node().style.backgroundColor = bgColor;                              
                          }
                          $thisCell.node().dataset["modified"] = true;
                      }
                  }
              }

              $(jqTableId).removeClass("editing");
          }
          if (runAtEndFocusOut != null)
          {
              runAtEndFocusOut(this);
          }
      });
  }

}


class BasicTable extends OpenDcsDataTable {
  constructor(domId, initialize) {
	  console.log("In constructor for Child Class Basic Table.");
	  var defaultProps = {
		        "searching": false,
		        "ordering": false,
		        "paging": false,
		        "info": false,
		        "scrollCollapse": true,
		        "scrollY": 150,
		        "autoWidth": true,
		        "dom": 'Bflrtip',
		        "buttons": []
		    };
	  var defaultInlineOptions = {
	          "columnDefs": [
	              {
	                  "targets": [0,1],
	                  "type": "input",
	                  "data": null,
	                  "bgcolor": {
	                      "change": "#c4eeff"
	                  }
	              }
	          ]
	      };
	  var defaultActions = [{
          "type": "delete",
          "onclick": null
      }];
	  super(domId, defaultProps, defaultInlineOptions, 
			  defaultActions, initialize);
  }
}

class PropertiesTable extends OpenDcsDataTable {
  constructor(domId, initialize) {
	  console.log("In constructor for Child Class Properties Table.");
	  var defaultProps = {
	          "searching": false,
	          "ordering": false,
	          "paging": false,
	          "info": false,
	          "dom": 'Bflrtip',
	          "scrollCollapse": true,
	          "autoWidth": true,
	          "scrollY": 150,
	          "buttons": []

	      };
	  var defaultInlineOptions = {
	          "columnDefs": [
	              {
	                  "targets": [0,1],
	                  "type": "input",
	                  "data": null,
	                  "bgcolor": {
	                      "change": "#c4eeff"
	                  }
	              }
	          ]
	      };
	  var defaultActions = [{
          "type": "delete",
          "onclick": null
      }];
	  super(domId, defaultProps, defaultInlineOptions, 
			  defaultActions, initialize);
	  this.propSpecMeta = {};
  }
  
  setPropspecs(propspecs)
  {
	  this.propspecs = propspecs;
	    if (propspecs == null)
	    {
	        propspecs = [];
	    }
	    for (var propSpecObj of propspecs)
	    {
	        this.propSpecMeta[propSpecObj.name] = {
	                "hover": propSpecObj.description,
	                "type": propSpecObj.type
	        }
	    }
  }
  
  updateProps(propertiesWithValues) {
	  var rowsWithValues = [];
	  var rowsWithoutValues = [];
	  
	  //Adds propspecs to passed properties object so they will show up in the 
	  //data table.
	  if (this.propSpecMeta != null)
	  {
		  for (var key in this.propSpecMeta)
		  {
			  if (propertiesWithValues[key] == null)
			  {
				  propertiesWithValues[key] = "";
			  }
		  }
	  }
	  
	  for (var key in propertiesWithValues)
	    {
	        var newRow = [key, propertiesWithValues[key], this.createActionDropdown(this.actions)];
	        if (newRow[1] != "")
	        {
	            rowsWithValues.push(newRow);
	        }
	        else
	        {
	            rowsWithoutValues.push(newRow);
	        }
	    }
	  //This causes the properties with values to go to the top.
	    for (var x = 0; x < rowsWithValues.length; x++)
	    {
	        this.addRow(rowsWithValues[x]);    
	    }
	    for (var x = 0; x < rowsWithoutValues.length; x++)
	    {
	        this.addRow(rowsWithoutValues[x]);    
	    }
	  //Need to draw first so that the "td" elements can be found.
	    this.dataTable.draw(false); 
	    if (this.inlineOptions == null)
	    {
	        //Prop table inline options null.
	    }

	    this.makeTableInline();
	    this.dataTable.draw(false);


	    var rowCount = this.dataTable.rows().count();
	    for (var x = 0; x < rowCount; x++)
	    {
	        var rowData = this.dataTable.row(x).data();
	        var propName = rowData[0];
	        if (this.propSpecMeta[propName] != null)
	        {
	            $(this.dataTable.row(x).node()).attr("title", this.propSpecMeta[propName].hover);
	            $($(this.dataTable.row(x).node()).find("td")[1]).attr("forced_propspec_type", this.propSpecMeta[propName].type);
	        }
	    }
  }
}