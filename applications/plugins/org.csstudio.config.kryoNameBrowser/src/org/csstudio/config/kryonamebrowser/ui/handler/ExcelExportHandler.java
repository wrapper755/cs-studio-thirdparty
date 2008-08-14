package org.csstudio.config.kryonamebrowser.ui.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.csstudio.config.kryonamebrowser.model.resolved.KryoNameResolved;
import org.csstudio.config.kryonamebrowser.ui.MainView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class ExcelExportHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		MainView view = (MainView) page.findView(MainView.ID);

		int i = 0;

		KryoNameResolved resolved;
		ArrayList<KryoNameResolved> list = new ArrayList<KryoNameResolved>();

		while ((resolved = (KryoNameResolved) view.getViewer()
				.getElementAt(i++)) != null) {
			list.add(resolved);
		}

		// File standard dialog
		FileDialog fileDialog = new FileDialog(HandlerUtil
				.getActiveShell(event));
		
		//TODO: change Open to Save.
		
		// Set the text
		fileDialog.setText("Save File");
		// Set filter on .txt files
		fileDialog.setFilterExtensions(new String[] { "*.xls" });
		// Put in a readable name for the filter
		fileDialog
				.setFilterNames(new String[] { "Excel 97-2003 format(*.xls)" });

		// Open Dialog and save result of selection
		String selected = fileDialog.open();

		if (!selected.isEmpty()) {
			File file = new File(selected);
			JOptionPane.showMessageDialog(null, file.getAbsolutePath());
			if (file.exists()) {

				boolean confirm = MessageDialog.openConfirm(HandlerUtil
						.getActiveShell(event), "Confirm", "Are you sure?");

				if (confirm) {

				}
			}else{
				
			}

			FileOutputStream outputStream = null;
			try {
				outputStream = new FileOutputStream(file);
				view.getLogic().excelExport(list, outputStream);

			} catch (FileNotFoundException e) {
				MessageDialog.openError(HandlerUtil.getActiveShell(event),
						"Error", e.getMessage());
			
			} catch (IOException e) {
				MessageDialog.openError(HandlerUtil.getActiveShell(event),
						"Error", e.getMessage());
			
			} finally {
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (IOException e) {
						// can't really do anything useful
					}
				}
			}

		}

		return null;
	}

}
