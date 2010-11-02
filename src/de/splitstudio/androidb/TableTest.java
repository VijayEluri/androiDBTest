package de.splitstudio.androidb;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import de.splitstudio.androidb.annotation.TableMetaData;

public class TableTest extends AndroidTestCase {

	private SQLiteDatabase db;

	private TableExample table;

	private static final String TABLE_NAME = TableExample.class.getSimpleName();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		String dbFilename = "test.db";
		getContext().getDatabasePath(dbFilename).delete();
		Table.createdTables.clear();
		table = new TableExample(getContext());
		db = table.getDb();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		table.drop();
	}

	public void testConstructorWithContext_createDbFile() {
		new TableExample(getContext());
		assertTrue(getContext().getDatabasePath(Table.DB_FILENAME).exists());
	}

	public void testConstructor_createTable() {
		assertEquals(1, TestHelper.getTableCount(TABLE_NAME, db));
	}

	public void testConstructor_tableCreated_noTableInDb() {
		table.drop();
		Table.createdTables.add(TABLE_NAME);
		new TableExample(getContext());
		assertEquals(0, TestHelper.getTableCount(TABLE_NAME, db));
	}

	public void testConstructor_tableWithNewId_callOnUpgrade() {
		table.drop();
		Metadata metadata = new Metadata(db);
		metadata.setTable(TABLE_NAME).setTableVersion(1).save();

		new TableExample(getContext());
		assertEquals(0, TestHelper.getTableCount(TABLE_NAME, db));

	public void testConstructor_createIndex() {
		Cursor c = db.query("SQLITE_MASTER", new String[] { "name" }, "type='index'", null, null, null, null);
		assertEquals(1, c.getCount());
		assertEquals(true, c.moveToFirst());
		assertEquals("index_TableExample", c.getString(c.getColumnIndex("name")));
		c.close();
	}

	public void test_isNew_withId_false() {
		table._id = 1L;
		assertEquals(false, table.isNew());
	}

	public void test_isNew_withoutId_true() {
		assertEquals(true, table.isNew());
	}

	public void test_isNew_withZeroId_true() {
		table._id = 0L;
		assertEquals(true, table.isNew());
	}

	public void test_all_emptyTable_emptyCursor() {
		Cursor c = table.all();
		assertEquals(0, c.getCount());
		c.close();
	}

	public void test_all_threeRows_threeRows() {
		table.insert();
		table._id = null;
		table.insert();
		table._id = null;
		table.insert();

		Cursor c = table.all();
		assertEquals(3, c.getCount());
		c.close();
	}

	public void test_find_withoutId_false() {
		assertEquals(false, table.find());
	}

	public void test_find_withoutZeroId_false() {
		table._id = 0L;
		assertEquals(false, table.find());
	}

	public void test_find_withId_trueAndFilled() {
		table.amount = 3.14f;
		table.text = "foo";
		table.save();
		Long id = table._id;

		TableExample tableDB = new TableExample(getContext());
		tableDB.find(id);

		assertEquals(table, tableDB);
	}

	public void test_insert_allColumnsInserted() {
		float amount = 3.14f;
		String text = "foo";
		table.amount = amount;
		table.text = text;
		table.insert();
		Long id = table._id;

		table = new TableExample(getContext());
		table.find(id);

		assertEquals(amount, table.amount);
		assertEquals(id, table._id);
		assertEquals(text, table.text);
	}

	public void test_delete_withoutId_false() {
		assertEquals(false, table.delete());
	}

	public void test_delete_idNotInDb_false() {
		table._id = 42L;
		assertEquals(false, table.delete());
	}

	public void test_delete_idInDb_true() {
		table._id = 42L;
		table.insert();
		assertEquals(true, table.delete());
	}

	public void test_update_nullId_false() {
		assertEquals(false, table.update());
	}

	public void test_update_withIdInDb_true() {
		table._id = 42L;
		table.insert();

		table.amount = 3.14f;
		assertEquals(true, table.update());

		table = new TableExample(getContext());
		table.find(42L);
		assertEquals(3.14f, table.amount);
	}

	public void test_save_noId_insert() {
		table.save();
		table = new TableExample(getContext());
		table.save();
		assertEquals(2, table.all().getCount());
	}

	public void test_save_id_update() {
		table.save();
		table.save();
		assertEquals(1, table.all().getCount());
	}

	public void test_drop_dropExistingTable_droppedAndRemovedFromMemoryAndMetadata() {
		table.drop();
		assertEquals(0, TestHelper.getTableCount(table.getTableName(), db));
		assertEquals(false, Table.createdTables.contains(table.getClass()));
		assertEquals(false, new Metadata(db).findByName(TABLE_NAME));
	}

	public void test_drop_dropNotExistingTable_noOneCares() {
		table.drop();
		table.drop();
	}

	public void test_equals_equalTable_true() {
		TableExample table2 = new TableExample(getContext());
		table._id = 42L;
		table.amount = 3.14f;
		table.text = new String("foo");
		table2._id = 42L;
		table2.amount = 3.14f;
		table2.text = new String("foo");
		assertEquals(table, table2);
	}

	public void test_equals_unequalTable_false() {
		TableExample table2 = new TableExample(getContext());
		table._id = 42L;
		table.amount = 3.14f;
		table.text = new String("foo");
		table2._id = 42L;
		table2.amount = 3.14001f;
		table2.text = new String("foo");
		assertEquals(false, table.equals(table2));
	}

	public void test_getVersion_noObjectVersion_exception() {
		class NotVersionedTable extends Table {
			NotVersionedTable(final Context context) {
				super(context);
			}
		}

		try {
			Table noVersion = new NotVersionedTable(getContext());
			noVersion.getVersion();
			fail("Expected to throw IllegalStateException");
		} catch (IllegalStateException e) {}
	}

	public void test_getVersion_objectVersionZero_exception() {
		@TableMetaData(version = 0)
		class NotVersionedTable extends Table {
			NotVersionedTable(final Context context) {
				super(context);
			}
		}

		try {
			Table noVersion = new NotVersionedTable(getContext());
			noVersion.getVersion();
			fail("Expected to throw IllegalStateException");
		} catch (IllegalStateException e) {}
	}

	public void test_fillFirst_emptyCursor_false() {
		Cursor c = table.all();
		assertEquals(false, table.fillFirst(c));
		c.close();
	}

	public void test_fillFirst_cursorMultipleRows_trueAndFirstFilled() {
		table.insert();
		table = new TableExample(getContext());
		table.insert();

		Cursor c = table.all();
		assertEquals(true, table.fillFirst(c));
		assertEquals(1l, (long) table._id);//this cast sucks!
		c.close();
	}

	public void test_getColumnNamesAsList() {
		List<String> expected = Arrays.asList("_id", "text", "amount");
		assertEquals(expected, table.getColumnNamesAsList());
	}

	public void test_getFields_someColumns_someColumns() {
		assertEquals(3, table.getFields().size());
	}

	public void test_insertWithSQLInjection_gotEscaped() {
		table.text = "'foobar;";
		assertTrue(table.insert());
	}

}
