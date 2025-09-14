/* Authored by iqbserve.de */

import { NL, newSimpleId, fileUtil, asDurationString } from '../jsmod/tools.mjs';
import { WsoCommonMessage, CommandDef } from '../jsmod/data-classes.mjs';
import { WorkView, ViewBuilder, onClicked, onInput } from '../jsmod/view-classes.mjs';
import { WorkbenchInterface as WbApp } from '../jsmod/workbench.mjs';
import * as Icons from '../jsmod/icons.mjs';

/**
 * A general View class for commands.
 *  - using web socket communication to execute server side "commands"
 *  - using a builder to create a the ui
 * 
 * Commands are either server side JavaScripts or java classes.
 * 
 * The client side command definitions are located in:
 *  - sidebar-content.mjs
 * The web socket counterpart on the server-side is:
 *  - org.isa.jps.comp.DefaultWebSocketMessageProcessor
 * The server side scripts are located in subdir
 *  - "scripts" for JS / and "extensions" for java
 * 
 */
class CommandView extends WorkView {
	//websocket communication ref id
	wsoRefId;

	commandDef = new CommandDef();
	commandName = "";
	runTime;
	duration;
	attachments = new Map();
	namedArgs = { none: "" };

	//input element for file dialog
	fileInput = null;

	//member objects to collect ui elements and ui objects from the builder
	elem = {};
	uiobj = {};

	constructor(id, file) {
		super(id, file);
		// example of using local html view source code
		// this.viewSource.setHtml(viewHtml);
	}

	onInstallation(installKey, installData, viewManager) {
		super.onInstallation(installKey, installData, viewManager);
		if (installData instanceof CommandDef) {
			this.commandDef = installData;
			this.commandName = this.commandDef.command + " " + this.commandDef.script;
		}
	}

	initialize() {
		super.initialize();
		this.setTitle(this.commandDef.title);

		//just demo data
		this.namedArgs = { help: "-h", testfile: "-file=test-data.json", cdata: '<![CDATA[ {"name":"HelloFunction", "args":["John Doe"]} ]]>' };

		this.viewHeader.menu((menu) => {
			menu.addItem("Clear Output", (evt) => {
				this.clearOutput();
			}, { separator: "top" });
			menu.addItem("Clear View", (evt) => {
				this.clearAll();
			});
		});

		this.fileInput = fileUtil.createFileInputElement("text/*, .json, .txt", (evt) => {
			let [file] = this.fileInput.files;
			this.fileInput.value = "";
			this.addAttachment(file);
		});

		this.createUI();
		this.createWsoConnection();

		this.isInitialized = true;
		this.setVisible(true);
	}

	/**
	 * <pre>
	 * build the ui using
	 * - an included viewHtml template
	 * - and a dom element builder 
	 * </pre>
	 */
	createUI() {
		let builder = new ViewBuilder()
			//set the objects to hold all control dom elements with a varid
			.setElementCollection(this.elem)
			// and other things like e.g. datalists or "data-bind" infos
			.setObjectCollection(this.uiobj)
			//set default styles
			.setCompPropDefaults((props) => {
				props.get("label").styleProps = { "width": "80px" };
			});

		//create a fieldset as component container in the view workarea
		//and save it to lacal variable
		let compSet;
		builder.newViewCompFor(this.viewWorkarea)
			.addFieldset({ pos: 0, styleProps: { "margin-top": "10px", "gap": "10px" } }, (target) => {
				compSet = target.fieldset;
			});

		builder.newViewComp()
			.addLabelButton({ text: "Command:" },
				{ varid: "pbRun", iconName: Icons.run(), text: this.commandName, title: "Run command" }, (target) => {
					onClicked(target.button, () => { this.runCommand() });
				})
			.appendTo(compSet);

		//arguments choice + demo data
		let namedArgsList = Object.getOwnPropertyNames(this.namedArgs);
		builder.newViewComp()
			.style({ "align-items": "flex-start" })
			.addLabelTextArea({ text: "Args:" }, {
				varid: "taArgs",
				styleProps: { "width": "400px", "min-width": "400px", "height": "45px", "min-height": "45px", "text-align": "left" },
				attribProps: {
					title: (this.commandDef.options.args ? "Command arguments: -h for help" : "<no args>") + "\nStructured text like e.g. JSON must be wrapped in a <![CDATA[ structured text ]]> tag.",
					placeholder: (this.commandDef.options.args ? " -h + Enter for help" : "<no args>"),
					disabled: !this.commandDef.options.args
				}
			}, (target) => {
				target.textarea.onkeydown = (evt) => {
					if (this.commandDef.options.args) {
						if (evt.keyCode == 13 && evt.currentTarget.value.trim() === "-h") {
							this.runCommand();
						}
					}
				}
			})
			.addColContainer({ styleProps: { "margin-left": "20px" } }, (target) => {
				target.comp.addTextField({
					varid: "tfNamedArgs", datalist: namedArgsList,
					styleProps: { "width": "200px" }, attribProps: { title: "Name of the defined arguments", placeholder: "named args", "data-bind": "namedArgs" }
				}).addRowContainer({ styleProps: { gap: "20px", "align-self": "flex-end", "margin-top": "5px" } }, (target) => {
					target.comp
						.addActionIcon({ varid: "icoDeleteNamedArgs", iconName: Icons.trash(), title: "Delete current named arg" })
						.addElement("span", { styleProps: { height: "20px", "border-right": "1px solid var(--border-gray)" } })
						.addActionIcon({ varid: "icoSaveNamedArgs", iconName: Icons.save(), title: "Save current named args" })
						.addActionIcon({ varid: "icoClearArgChoice", iconName: Icons.eraser(), title: "Clear args and choice", styleProps: { "margin-left": "20px", "margin-right": "5px" } });
				})
			})
			.appendTo(compSet);

		//attachment list
		builder.newViewComp()
			.style({ "align-items": "flex-start" })
			.addLabel({ text: "Attachments:" })
			.addColContainer((target) => {
				target.comp
					.addRowContainer({ styleProps: { gap: "20px", "align-self": "flex-start" } }, (target) => {
						target.comp
							.addActionIcon({ varid: "icoRemoveAllAttachments", iconName: Icons.trash(), title: "Remove all Attachments" })
							.addActionIcon({ varid: "icoAddAttachment", iconName: Icons.plusNew(), title: "Add Attachment" });
					}).addList({
						varid: "lstAttachments",
						styleProps: { "min-width": "385px", "min-height": "20px", "padding": "10px" }
					});
			})
			.appendTo(compSet);

		//a separator
		builder.newViewComp().addSeparator({ styleProps: { width: "100%" } }).appendTo(compSet);

		//the output area
		builder.newViewComp()
			.style({ "align-items": "flex-start" })
			.addColContainer({ styleProps: { "align-items": "center", "gap": "15px" } }, (target) => {
				let comp = target.comp;

				comp.addLabelTextArea(
					{ text: "Output:", },
					{ varid: "taOutput", parent: comp.parent, disabled: true, clazzes: "wkv-output-textarea-ctrl", styleProps: { width: "626px", "min-width": "626px" } })
					.addActionIcon({ varid: "icoOutputSave", iconName: Icons.save(), title: "Save current output to a file" }, (target) => {
						onClicked(target.icon, () => { this.saveOutput(); });
					})
					.addActionIcon({ varid: "icoOutputToClipboard", iconName: Icons.clipboardAdd(), title: "Copy current output to clipboard" }, (target) => {
						onClicked(target.icon, () => { this.copyOutputToClipboard(); });
					})
					.addActionIcon({ varid: "icoOutputDelete", iconName: Icons.trash(), title: "Delete current output" }, (target) => {
						onClicked(target.icon, () => { this.clearOutput(); });
					});
			})
			.appendTo(compSet);

		onInput(this.elem.tfNamedArgs, (evt) => {
			let key = evt.currentTarget.value;
			this.setArgsSelection(key);
		});
		onClicked(this.elem.icoClearArgChoice, () => { this.clearArgChoice(); });
		onClicked(this.elem.icoDeleteNamedArgs, () => { this.deleteArgChoice(); });
		onClicked(this.elem.icoSaveNamedArgs, () => { this.saveArgChoice(); });
		onClicked(this.elem.icoAddAttachment, () => { this.fileInput.click(); });
		onClicked(this.elem.icoRemoveAllAttachments, () => { this.removeAllAttachments(); });
	}

	createWsoConnection() {
		this.wsoRefId = newSimpleId(this.id + ":");
		WbApp.addWsoMessageListener((wsoMsg) => {

			if (wsoMsg.hasReference(this.wsoRefId)) {
				if (wsoMsg.hasStatusSuccess()) {
					this.setRunning(false);
					this.addOutputLine(NL + `Command finished: [${wsoMsg.status}] [${this.commandName}] [${asDurationString(this.runTime)}]`);
				} else if (wsoMsg.hasStatusError()) {
					this.addOutputLine(NL + wsoMsg.error);
					this.setRunning(false);
				} else {
					this.addOutputLine(wsoMsg.bodydata);
				}
			} else if (wsoMsg.hasStatusError && wsoMsg.error.includes("connection")) {
				this.addOutputLine(NL + wsoMsg.error);
				this.setRunning(false);
			} else if (wsoMsg.hasStatusError && wsoMsg.hasReference("server.global")) {
				this.addOutputLine(NL + "WebSocket Error [" + wsoMsg.error + "] the central connection was closed.");
				this.setRunning(false);
			}
		});
	}

	clearAll() {
		this.clearArgChoice();
		this.clearOutput();
		this.removeAllAttachments();

		//resize elements
		this.elem.taArgs.style.width = "0px";
		this.elem.taArgs.style.height = "0px";
		this.elem.taOutput.style.width = "0px";
		this.elem.taOutput.style.height = "0px";
	}

	setRunning(flag) {
		super.setRunning(flag);
		this.elem.pbRun.disabled = flag;
		if(flag){
			this.runTime = Date.now();
		}else{
			this.runTime = Date.now()-this.runTime;
		}
	}

	runCommand() {
		let wsoMsg = new WsoCommonMessage(this.wsoRefId);
		wsoMsg.command = this.commandDef.command;
		wsoMsg.functionModule = this.commandDef.script;
		wsoMsg.argsSrc = this.elem.taArgs.value.trim();

		if (this.attachments.size > 0) {
			this.attachments.forEach(function (value, key) {
				wsoMsg.addAttachment(value.name, value.data);
			})
		}

		this.clearOutput();
		WbApp.sendWsoMessage(wsoMsg, () => {
			this.setRunning(true);
		});
	}

	addOutputLine(line) {
		this.elem.taOutput.value += line + NL;
		this.elem.taOutput.scrollTop = this.elem.taOutput.scrollHeight;
	}

	setArgsSelection(key) {
		if (this.commandDef.options.args && this.namedArgs[key]) {
			this.elem.taArgs.value = this.namedArgs[key];
		}
	}

	clearArgChoice() {
		this.elem.taArgs.value = "";
		this.elem.tfNamedArgs.value = "";
	}

	getDataListObjFor(name) {
		return this.uiobj[this.elem[name].list.id];
	}

	saveArgChoice() {
		let key = this.elem.tfNamedArgs.value.trim();
		if (key != "") {
			this.namedArgs[key] = this.elem.taArgs.value.trim();
			let datalist = this.getDataListObjFor("tfNamedArgs");
			datalist.addOption(key);
		}
	}

	deleteArgChoice() {
		let key = this.elem.tfNamedArgs.value.trim();

		if (this.namedArgs[key]) {
			WbApp.confirm({
				message: `<b>Delete entry</b><br>Do you want to delete <b>[${key}]</b> from arg choice?`
			}, (val) => {
				if (val) {
					delete this.namedArgs[key];
					let dataListObj = this.getDataListObjFor("tfNamedArgs");
					dataListObj.removeOption(key);
					this.clearArgChoice();
				}
			});
		}
	}

	saveOutput() {
		let fileName = "output_" + (this.commandDef.command + "_" + this.commandDef.script).replaceAll("/", "_") + ".txt";
		this.saveToFile(fileName, this.elem.taOutput.value.trim());
	}

	copyOutputToClipboard() {
		this.copyToClipboard(this.elem.taOutput.value.trim());
	}

	clearOutput() {
		if (!this.state.isRunning) {
			let lastValue = this.elem.taOutput.value;
			this.elem.taOutput.value = "";
			return lastValue;
		}
		return "";
	}

	addAttachment(file) {
		if (file && !this.attachments.has(file.name)) {
			let attachment = new Attachment(file.name);
			let reader = new FileReader();
			reader.onload = (e) => {
				attachment.data = e.target.result;
			};
			reader.readAsText(file);

			this.attachments.set(file.name, attachment);
			this.addAttachmentToList(attachment);
		}
	}

	addAttachmentToList(attachment) {
		let item = document.createElement("li");
		item.classList.add("indexed");
		let iconClazzes = Icons.getIconClasses(Icons.xRemove(), true);
		let html = `<span class='${iconClazzes} wkv-listitem-ctrl' title='Remove Attachment' style='margin-right: 20px;'></span> <span>${attachment.name}</span>`;
		item.innerHTML = html;

		this.elem.lstAttachments.appendChild(item);
		onClicked(item.firstChild, (evt) => {
			let name = evt.target.parentElement.lastChild.textContent;
			this.removeAttachmentFromList(name, item);
		});
	}

	removeAttachmentFromList(name, item) {
		this.elem.lstAttachments.removeChild(item);
		this.attachments.delete(name);
	}

	removeAllAttachments() {
		this.attachments = new Map();
		let list = this.elem.lstAttachments;
		while (list.firstChild) list.removeChild(list.firstChild);
	}

}

class Attachment {
	name;
	data;
	constructor(name) {
		this.name = name;
	}
}

//export this view component as individual instances
//the view will get specified by a CommandDef data object
export function getView(id = "") {
	return new CommandView(id, "/jsmod/html-components/work-view.html");
}



/**
 * NOT active example of using local html source code integrated as string
 * see constructor
 */
let viewHtml = `
<!-- js component based work view code -->
<div class="work-view" style="visibility: hidden;">

	<div class="work-view-header-container">

		<div class="work-view-header">

			<!-- the standard header icons left -->
			<span id="wkv.header.iconbar.left" class="header-iconbar header-left"></span>

			<!-- the view popup menu -->
			<div id="header.menu" class="wkv-header-menu"></div>

			<!-- the title -->
			<span id="view.title" class="wkv-header-title">Unknown</span>

			<!-- the standard header icons right -->
			<span id="wkv.header.iconbar.right" class="header-iconbar header-right"></span>

		</div>

		<div id="wkv.header.prgrogressbar" class="wkv-header-progressbar">
			<div class="progress-value"></div>
		</div>
	</div>

	<div id="work.view.body" class="work-view-body">
		<!-- the view working/content area -->
		<div id="work.view.workarea" class="work-view-workarea flex-one"></div>

		<div id="work.view.sidepanel.splitter" class="vsplitter wkv-sidepanel-splitter"></div>

		<div id="work.view.sidepanel" class="work-view-sidepanel"></div>
	</div>

</div>
<!-- work view end -->
`;

