package org.csstudio.config.kryonamebrowser.logic;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.csstudio.config.kryonamebrowser.config.OracleSettings;
import org.csstudio.config.kryonamebrowser.config.Settings;
import org.csstudio.config.kryonamebrowser.database.DBConnect;
import org.csstudio.config.kryonamebrowser.database.TableNames;
import org.csstudio.config.kryonamebrowser.model.entry.KryoNameEntry;
import org.csstudio.config.kryonamebrowser.model.entry.KryoObjectEntry;
import org.csstudio.config.kryonamebrowser.model.entry.KryoPlantEntry;
import org.csstudio.config.kryonamebrowser.model.entry.KryoProcessEntry;
import org.csstudio.config.kryonamebrowser.model.resolved.KryoNameResolved;
import org.csstudio.config.kryonamebrowser.model.resolved.KryoPlantResolved;

/**
 * Main logic for the name browser.
 * 
 * @author Alen Vrecko
 */
public class KryoNameBrowserLogic {

	private static final int DESCRIPTION_LENGTH = 200;
	private DBConnect database;
	public static final int NO_PARENT_PLANT_ID = 1;
	public static final int NO_PARENT_SUPER_PLANT_ID = 0;
	public static final int NO_PARENT_OBJECT_ID = 0;
	public static final int ROW_FETCH_SIZE = 50;
	private static short NAME_CELL_WIDTH = 4000;

	private static short NOM_CELL_WIDTH = 1000;

	public static void main(String[] args) throws InterruptedException {
		for (int i = 0; i < 30; i++) {
			new Thread(new Runnable() {
				public void run() {

					KryoNameBrowserLogic logic = new KryoNameBrowserLogic(
							new OracleSettings());

					try {
						logic.openConnection();
						for (int i = 0; i < 100; i++) {

							long l = System.currentTimeMillis();

							KryoNameResolved resolved = new KryoNameResolved();
							resolved.getPlants().add(new KryoPlantResolved(4));
							logic.search(resolved);

							System.out.println(""
									+ Thread.currentThread().getName() + " "
									+ (System.currentTimeMillis() - l));

							try {
								Thread.sleep((long) (Math.random() * 10000));
							} catch (InterruptedException e) {
								e.printStackTrace(); // To change body of catch
								// statement use File |
								// Settings | File
								// Templates.
							}

						}

					} catch (SQLException e) {
						e.printStackTrace();

					}

					try {
						logic.closeConnection();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}).start();

		}
	}

	public void closeConnection() throws SQLException {
		database.closeConnection();

	}

	public KryoNameBrowserLogic(Settings settings) {
		this.database = new DBConnect(settings);
	}

	/**
	 * Opens the underlying database connection. You should call this method once before using the methods on this
	 * class.
	 * 
	 * @throws SQLException
	 */
	public synchronized void openConnection() throws SQLException {
		database.openConnection();
	}

	private String getQueryForExample(KryoNameResolved example) {
		StringBuilder nameQuery = new StringBuilder();

		// handle plants filtering
		List<KryoPlantResolved> plantsExample = example.getPlants();
		boolean isUsed = false;
		if (plantsExample.size() > 0) {
			isUsed = true;
			// TODO: Handle better? HARD-CODED VALUE
			nameQuery.append("X");
			for (KryoPlantEntry kryoPlantEntry : plantsExample) {
				nameQuery.append(kryoPlantEntry.getLabel());

				if (kryoPlantEntry.getNumberOfPlants() > 0) {
					nameQuery.append(kryoPlantEntry.getNumberOfPlants());
				} else {
					nameQuery.append("%");
				}
			}

		}

		nameQuery.append("%:%");

		// handle objects entry
		List<KryoObjectEntry> objectsExample = example.getObjects();
		if (objectsExample.size() > 0) {
			isUsed = true;
			// remove the last one which is '%'
			nameQuery.deleteCharAt(nameQuery.length() - 1);

			for (KryoObjectEntry kryoObjectEntry : objectsExample) {
				nameQuery.append(kryoObjectEntry.getLabel());
			}
			nameQuery.append("%");

		}

		// handle process and seq kryo number

		if (example.getProcess() != null) {
			isUsed = true;
			nameQuery.append(example.getProcess().getId());
		}

		if (example.getSeqKryoNumber() >= 0) {
			isUsed = true;
			int number = example.getSeqKryoNumber();
			nameQuery.append(number < 10 ? "0" + number : number);
		} else {
			nameQuery.append("__");
		}

		JOptionPane.showMessageDialog(null, nameQuery.toString());

		return isUsed ? nameQuery.toString() : "";
	}

	/**
	 * List of all {@link KryoNameResolved} which are subsets of the example.
	 * 
	 * @param example
	 *            entry used for comparison
	 * @return list of all resolved objects
	 * @throws SQLException
	 *             if something went wrong with the database
	 */
	public synchronized List<KryoNameResolved> search(KryoNameResolved example)
			throws SQLException {

		StringBuffer selectQuery = new StringBuffer(
				"SELECT IO_NAME_ID , IO_NAME , PLANT_ID , OBJECT_ID , CRYO_PROCESS_ID , SEQ_KRYO_NUMBER , KRYO_NAME_LABEL FROM ")
				.append(TableNames.NAMES_TABLE);

		String nameQuery = getQueryForExample(example);

		if (nameQuery.length() > 0) {
			selectQuery.append("  WHERE  io_name like '").append(
					nameQuery.toString()).append("'");
		}

		ArrayList<KryoNameResolved> results = new ArrayList<KryoNameResolved>();
		Statement statement = null;
		ResultSet resultSet = null;
		try {
			statement = database.getConnection().createStatement();
			statement.setFetchSize(ROW_FETCH_SIZE);

			resultSet = statement.executeQuery(selectQuery.toString());

			HashMap<Integer, KryoObjectEntry> objectCache = new HashMap<Integer, KryoObjectEntry>();
			HashMap<Integer, KryoPlantEntry> plantCache = new HashMap<Integer, KryoPlantEntry>();

			while (resultSet.next()) {

				// resolve all but the types that have subtypes
				KryoNameResolved kryoNameResolved = new KryoNameResolved(
						resultSet.getString(2), resultSet.getString(7),
						resultSet.getInt(1), resultSet.getInt(6),
						getProcessEntry(resultSet.getString(5)));

				// resolve the subtypes into a list
				List<KryoObjectEntry> objects = kryoNameResolved.getObjects();

				KryoObjectEntry kryoObjectEntry = getObjectEntry(resultSet
						.getInt(4), objectCache);

				while (kryoObjectEntry != null) {

					objects.add(kryoObjectEntry);
					kryoObjectEntry = getObjectEntry(kryoObjectEntry
							.getParent(), objectCache);
				}

				Collections.reverse(objects);

				List<KryoPlantResolved> plants = kryoNameResolved.getPlants();

				KryoPlantEntry kryoPlantEntry = getPlantEntry(resultSet
						.getInt(3), plantCache);

				// split by ':' in two halves, use the left halve to check
				// numbers

				String[] split = kryoNameResolved.getName().split(":");

				String[] plantHalve = split[0].split("[A-Z]+");

				int plantHalveIndex = plantHalve.length - 1;

				while (kryoPlantEntry != null) {
					int nrOfPlants = -1;

					// do we allow plants, if so parse the int.
					if (kryoPlantEntry.getNumberOfPlants() > 0) {

						try {
							nrOfPlants = Integer
									.parseInt(plantHalve[plantHalveIndex]);

						} catch (Exception e) {
							JOptionPane.showMessageDialog(null,
									"Invalid name entry found "
											+ kryoNameResolved.getName()
											+ " please notify supervisor!");
						}

						plantHalveIndex--;
					}

					KryoPlantResolved plantResolved = new KryoPlantResolved(
							kryoPlantEntry);
					plantResolved.setNumberOfPlants(nrOfPlants);

					plants.add(plantResolved);
					kryoPlantEntry = getPlantEntry(kryoPlantEntry.getParent(),
							plantCache);
				}

				Collections.reverse(plants);

				results.add(kryoNameResolved);
			}

		} finally {

			if (statement != null) {
				statement.close();
			}

		}
		return results;

	}

	private KryoProcessEntry getProcessEntry(String id) throws SQLException {
		Statement statement = database.getConnection().createStatement();
		ResultSet resultSet = statement
				.executeQuery("select CRYO_PROCESS_NAME, CRYO_PROCESS_ID, CRYO_PROCESS_EXPLANATION "
						+ "from NSB_CRYO_PROCESS where NSB_CRYO_PROCESS.CRYO_PROCESS_ID = "
						+ id);

		try {
			if (resultSet.next()) {
				return new KryoProcessEntry(resultSet.getString(1), resultSet
						.getString(2), resultSet.getString(3));
			} else {
				throw new RuntimeException("Missing process for id " + id);
			}
		} finally {
			statement.close();
		}
	}

	private KryoObjectEntry getObjectEntry(int id,
			HashMap<Integer, KryoObjectEntry> objectCache) throws SQLException {
		if (id == NO_PARENT_OBJECT_ID) {
			return null;
		}

		if (objectCache.containsKey(id)) {

			return new KryoObjectEntry(objectCache.get(id));
		}

		Statement statement = database.getConnection().createStatement();
		ResultSet resultSet = statement
				.executeQuery("Select OBJECT_NAME, OBJECT_EXPLANATION, OBJECT_ID, OBJECT_PARENT, OBJECT_LABEL , OBJECT_LEVEL from NSB_OBJECT where NSB_OBJECT.OBJECT_ID = "
						+ id);

		try {
			if (resultSet.next()) {
				KryoObjectEntry entry = new KryoObjectEntry(resultSet
						.getString(1), resultSet.getString(2), resultSet
						.getInt(3), resultSet.getInt(4),
						resultSet.getString(5), resultSet.getInt(6));
				objectCache.put(id, entry);
				return entry;
			} else {
				throw new RuntimeException("Missing object for id " + id);
			}
		} finally {
			statement.close();
		}
	}

	private KryoPlantEntry getPlantEntry(int id,
			HashMap<Integer, KryoPlantEntry> plantCache) throws SQLException {
		if (id == NO_PARENT_PLANT_ID) {
			return null;
		}

		if (plantCache.containsKey(id)) {
			return plantCache.get(id);
		}

		Statement statement = database.getConnection().createStatement();
		ResultSet resultSet = statement
				.executeQuery("select PLANT_NAME, PLANT_LABEL, PLANT_EXPLANATION, PLANT_ID,"
						+ " PLANT_PARENT, PLANT_NO from NSB_PLANT where NSB_PLANT.PLANT_ID = "
						+ id);

		try {
			if (resultSet.next()) {
				KryoPlantEntry kryoPlantEntry = new KryoPlantEntry(resultSet
						.getString(1), resultSet.getString(2), resultSet
						.getString(3), resultSet.getInt(4),
						resultSet.getInt(5), resultSet.getInt(6));
				plantCache.put(id, kryoPlantEntry);
				return kryoPlantEntry;
			} else {
				throw new RuntimeException("Missing plant for id" + id);
			}
		} finally {
			statement.close();
		}
	}

	private boolean isLowestLevelPlant(int id) throws SQLException {
		Statement statement = database.getConnection().createStatement();
		boolean next = false;
		try {
			next = statement.executeQuery(
					"select PLANT_NAME from NSB_PLANT where PLANT_PARENT='"
							+ id + "'").next();

		} finally {
			statement.close();
		}

		return !next;

	}

	private boolean isLowestLevelObject(int id) throws SQLException {
		Statement statement = database.getConnection().createStatement();
		boolean next = false;
		try {
			next = statement.executeQuery(
					"select OBJECT_NAME from NSB_OBJECT where OBJECT_PARENT='"
							+ id + "'").next();

		} finally {
			statement.close();
		}

		return !next;

	}

	public synchronized void add(KryoNameEntry newEntry) throws SQLException {

		if (newEntry.getName() == null || newEntry.getName().isEmpty()
				|| newEntry.getProcessId() == null
				|| newEntry.getProcessId().isEmpty()) {
			throw new IllegalStateException("Missing name or process");
		}

		if (doesExist(newEntry.getName())) {
			throw new IllegalStateException("Cannot add already existing name");
		}

		Statement statement = database.getConnection().createStatement();

		try {

			// make sure that numbers are not set where there are not allowed
			// and the only the lowest level is added.

			if (!isLowestLevelPlant(newEntry.getPlantId())) {
				throw new RuntimeException("Validation failed");
			}

			if (!isLowestLevelObject(newEntry.getObjectId())) {
				throw new RuntimeException("Validation failed");
			}

			// TODO: validation of numbers set is quite difficult, if time will
			// add later also can validate last two entries in name (process and sequence).

			statement
					.executeUpdate("insert into NSB_IO_NAME (IO_NAME, PLANT_ID, OBJECT_ID, CRYO_PROCESS_ID, "
							+ "SEQ_KRYO_NUMBER, KRYO_NAME_LABEL) values ('"
							+ newEntry.getName()
							+ "','"
							+ newEntry.getPlantId()
							+ "','"
							+ newEntry.getObjectId()
							+ "','"
							+ newEntry.getProcessId()
							+ "','"
							+ newEntry.getSeqKryoNumber()
							+ "','"
							+ newEntry.getLabel() + "')");

		} finally {
			statement.close();
		}
	}

	public synchronized void delete(KryoNameEntry kryoNameEntry)
			throws SQLException {
		Statement statement = database.getConnection().createStatement();
		try {
			statement.executeUpdate("delete from NSB_IO_NAME where IO_NAME_ID = '"
					+ kryoNameEntry.getId() + "'");
		} finally {
			statement.close();
		}
	}

	/**
	 * The entry's name will be used to update the label from the corresponding name entry in the database.
	 * 
	 * @param kryoNameEntry
	 *            entry from which to use the name and new label
	 */
	public synchronized void updateLabel(KryoNameEntry kryoNameEntry)
			throws SQLException {

		Statement statement = database.getConnection().createStatement();

		try {
			statement
					.executeUpdate("update NSB_IO_NAME set KRYO_NAME_LABEL = '"
							+ kryoNameEntry.getLabel() + "' where IO_NAME_ID = '"
							+ kryoNameEntry.getId() + "'");

		} finally {
			statement.close();
		}

	}

	/**
	 * Checks if the name already exists.
	 * 
	 * @param name
	 *            name of the PV
	 * @return true if exists
	 */
	public synchronized boolean doesExist(String name) throws SQLException {
		Statement statement = database.getConnection().createStatement();
		boolean next = false;
		try {
			next = statement.executeQuery(
					"select IO_NAME from NSB_IO_NAME where IO_NAME='" + name
							+ "'").next();

		} finally {
			statement.close();
		}

		return next;

	}

	/**
	 * Returns a list of {@link KryoObjectEntry} where their parent is 0.
	 * 
	 * @return list of toplevel entries
	 */
	public synchronized List<KryoObjectEntry> findToplevelObjectChoices()
			throws SQLException {
		return findObjectChoices(new KryoObjectEntry(NO_PARENT_OBJECT_ID));
	}

	/**
	 * Returns a list of {@link KryoObjectEntry} where their parent is specified.
	 * 
	 * @param parent
	 *            entry
	 * @return list of entries corresponding to the parent
	 */
	public synchronized List<KryoObjectEntry> findObjectChoices(
			KryoObjectEntry parent) throws SQLException {

		Statement statement = database.getConnection().createStatement();
		statement.setFetchSize(ROW_FETCH_SIZE);

		ArrayList<KryoObjectEntry> entries;
		try {

			ResultSet rs = statement
					.executeQuery("select OBJECT_NAME, OBJECT_EXPLANATION, OBJECT_ID, OBJECT_PARENT,"
							+ " OBJECT_LABEL , OBJECT_LEVEL from NSB_OBJECT where NSB_OBJECT.OBJECT_PARENT = "
							+ parent.getId());

			entries = new ArrayList<KryoObjectEntry>();

			while (rs.next()) {
				entries.add(new KryoObjectEntry(rs.getString(1), rs
						.getString(2), rs.getInt(3), rs.getInt(4), rs
						.getString(5), rs.getInt(6)));
			}

		} finally {
			statement.close();
		}

		return entries;
	}

	/**
	 * Returns a list of {@link KryoPlantEntry} where their parent is 0.
	 * 
	 * @return list of toplevel entries
	 * @throws java.sql.SQLException
	 */
	public synchronized List<KryoPlantEntry> findToplevelPlantChoices()
			throws SQLException {
		return findPlantChoices(new KryoPlantEntry(NO_PARENT_PLANT_ID));
	}

	/**
	 * Returns a list of {@link KryoPlantEntry} where their parent is specified.
	 * 
	 * @param parent
	 *            entry
	 * @return list of entries corresponding to the parent
	 * @throws java.sql.SQLException
	 */
	public synchronized List<KryoPlantEntry> findPlantChoices(
			KryoPlantEntry parent) throws SQLException {

		List<KryoPlantEntry> results = new ArrayList<KryoPlantEntry>();
		Statement statement = database.getConnection().createStatement();
		statement.setFetchSize(ROW_FETCH_SIZE);

		try {
			ResultSet rs = statement
					.executeQuery("select PLANT_NAME, PLANT_LABEL, PLANT_EXPLANATION, PLANT_ID,"
							+ " PLANT_PARENT, PLANT_NO from NSB_PLANT where NSB_PLANT.PLANT_PARENT = "
							+ parent.getId());
			while (rs.next()) {
				results.add(new KryoPlantEntry(rs.getString(1),
						rs.getString(2), rs.getString(3), rs.getInt(4), rs
								.getInt(5), rs.getInt(6)));
			}
		} finally {
			statement.close();
		}

		return results;
	}

	/**
	 * Returns the top level {@link KryoPlantEntry} that should be the XFEL.
	 * 
	 * @param parent
	 *            entry
	 * @return entry
	 * @throws java.sql.SQLException
	 */
	public synchronized KryoPlantEntry getSuperPlant() throws SQLException {

		List<KryoPlantEntry> results = new ArrayList<KryoPlantEntry>();
		Statement statement = database.getConnection().createStatement();
		statement.setFetchSize(ROW_FETCH_SIZE);

		try {
			ResultSet rs = statement
					.executeQuery("select PLANT_NAME, PLANT_LABEL, PLANT_EXPLANATION, PLANT_ID,"
							+ " PLANT_PARENT, PLANT_NO from NSB_PLANT where NSB_PLANT.PLANT_PARENT = "
							+ NO_PARENT_SUPER_PLANT_ID);
			if (rs.next()) {
				return new KryoPlantEntry(rs.getString(1), rs.getString(2), rs
						.getString(3), rs.getInt(4), rs.getInt(5), rs.getInt(6));
			} else {
				return null;
			}
		} finally {
			statement.close();
		}

	}

	/**
	 * Returns a list of {@link KryoProcessEntry}.
	 * 
	 * @return list of all process entries.
	 * @throws java.sql.SQLException
	 */
	public synchronized List<KryoProcessEntry> findProcessChoices()
			throws SQLException {

		List<KryoProcessEntry> results = new ArrayList<KryoProcessEntry>();

		Statement statement = database.getConnection().createStatement();
		statement.setFetchSize(ROW_FETCH_SIZE);
		try {
			ResultSet rs = statement
					.executeQuery("select CRYO_PROCESS_NAME, CRYO_PROCESS_ID, CRYO_PROCESS_EXPLANATION "
							+ "from NSB_CRYO_PROCESS");

			while (rs.next()) {
				results.add(new KryoProcessEntry(rs.getString(1), rs
						.getString(2), rs.getString(3)));
			}

		} finally {
			statement.close();
		}

		return results;
	}

	private boolean isEmpty(String string) {
		return string == null || "".equals(string);
	}

	/**
	 * Exports the given list to Excel. Make sure you properly close the stream. This method does not close the stream.
	 * 
	 * @param list
	 * @param fileInputStream
	 * @throws IOException
	 */
	public void excelExport(ArrayList<KryoNameResolved> list,
			OutputStream outputStream) throws IOException {

		short rownum = 0;

		// create a new file
		// create a new workbook
		HSSFWorkbook wb = new HSSFWorkbook();
		// create a new sheet
		HSSFSheet s = wb.createSheet();
		// declare a row object reference
		HSSFRow r = null;
		// declare a cell object reference
		HSSFCell c = null;
		// create a cell style
		HSSFCellStyle csCapital = wb.createCellStyle();

		HSSFCellStyle csNormal = wb.createCellStyle();
		// create a font object
		HSSFFont fontCapital = wb.createFont();
		HSSFFont fontNormal = wb.createFont();

		// set font 1 to 12 point type
		fontCapital.setFontHeightInPoints((short) 10);
		fontCapital.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);

		// make it blue
		// f.setColor( (short)0xc );
		// make it bold
		// arial is the default font
		// f.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);

		csCapital.setFont(fontCapital);

		// set the sheet name
		wb.setSheetName(0, "KryoNames",
				HSSFWorkbook.ENCODING_COMPRESSED_UNICODE);

		// setCaption(s, r, c);
		// create a row
		r = s.createRow(rownum);

		// create cells
		c = r.createCell((short) 0);
		// set this cell to the first cell style we defined
		c.setCellStyle(csCapital);
		// set the cell's string value to "Test"
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("Kryo Name");
		s.setColumnWidth((short) 0, NAME_CELL_WIDTH);

		c = r.createCell((short) 1);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("Plant");
		s.setColumnWidth((short) 1, NAME_CELL_WIDTH);

		c = r.createCell((short) 2);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("No");
		s.setColumnWidth((short) 2, NOM_CELL_WIDTH);

		c = r.createCell((short) 3);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("Sub Plant 1");
		s.setColumnWidth((short) 3, NAME_CELL_WIDTH);

		c = r.createCell((short) 4);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("No");
		s.setColumnWidth((short) 4, NOM_CELL_WIDTH);

		c = r.createCell((short) 5);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("Sub Plant 2");
		s.setColumnWidth((short) 5, NAME_CELL_WIDTH);

		c = r.createCell((short) 6);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("No");
		s.setColumnWidth((short) 6, NOM_CELL_WIDTH);

		c = r.createCell((short) 7);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("Sub Plant 3");
		s.setColumnWidth((short) 7, NAME_CELL_WIDTH);

		c = r.createCell((short) 8);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("No");
		s.setColumnWidth((short) 8, NOM_CELL_WIDTH);

		c = r.createCell((short) 9);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("Object");
		s.setColumnWidth((short) 9, NAME_CELL_WIDTH);

		c = r.createCell((short) 10);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("Object Function");
		s.setColumnWidth((short) 10, NAME_CELL_WIDTH);

		c = r.createCell((short) 11);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("Object Subfunction");
		s.setColumnWidth((short) 11, NAME_CELL_WIDTH);

		c = r.createCell((short) 12);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("Process Part");
		s.setColumnWidth((short) 12, NAME_CELL_WIDTH);

		c = r.createCell((short) 13);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("Seq No");
		s.setColumnWidth((short) 13, (short) 1700);

		c = r.createCell((short) 14);
		c.setCellStyle(csCapital);
		c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);
		c.setCellValue("Description");
		s.setColumnWidth((short) 14, (short) 3000);

		fontNormal.setFontHeightInPoints((short) 10);

		csNormal.setFont(fontNormal);

		// create a sheet with rows
		// for (rownum = (short) 1; rownum < sd.getKryoNameList().size(); rownum++)
		for (KryoNameResolved resolved : list) {

			rownum++;
			// create a row
			r = s.createRow(rownum);
			// create cells
			c = r.createCell((short) 0);
			c.setCellValue(resolved.getName());

			// c.setEncoding(HSSFCell.ENCODING_COMPRESSED_UNICODE);

			List<KryoPlantResolved> plants = resolved.getPlants();

			int i = 1;
			for (KryoPlantEntry kryoPlantEntry : plants) {
				c = r.createCell((short) i++);
				c.setCellValue(kryoPlantEntry.getName());
				c = r.createCell((short) i++);
				int numberOfPlants = kryoPlantEntry.getNumberOfPlants();

				c.setCellValue(numberOfPlants < 0 ? "" : "" + numberOfPlants);

			}

			i = 9;

			List<KryoObjectEntry> objects = resolved.getObjects();

			for (KryoObjectEntry kryoObjectEntry : objects) {
				c = r.createCell((short) i++);
				c.setCellValue(kryoObjectEntry.getName());

			}

			c = r.createCell((short) 12);
			c.setCellValue(resolved.getProcess().getName());

			c = r.createCell((short) 13);
			c.setCellValue(resolved.getSeqKryoNumber());

			c = r.createCell((short) 14);
			String label = resolved.getLabel();
			c.setCellValue(label != null ? label.substring(0, Math.min(
					DESCRIPTION_LENGTH, label.length())) : "");
		}

		// write the workbook to the output stream
		// close our file (don't blow out our file handles
		wb.write(outputStream);

	}

}