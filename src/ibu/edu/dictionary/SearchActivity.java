package ibu.edu.dictionary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import android.app.Dialog;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;

public class SearchActivity extends SherlockActivity {

	EditText searchText;
	Button buttonDetails;
	ListView wordsList;
	SQLiteAssetHelper myDBHelper;
	int textlength = 0;
	ArrayList<Word> words = new ArrayList<Word>();
	ArrayList<Word> wordsTurkish = new ArrayList<Word>();
	ArrayList<Word> wordsBosnian = new ArrayList<Word>();
	ArrayList<Word> wordsEnglish = new ArrayList<Word>();
	ArrayList<Word> wordsAfterSearch = new ArrayList<Word>();
	WordAdapter wordAdapter;
	boolean isFirstEnglish = true, isFirstBosnian = true;
	long idle_min = 700;
	long last_text_edit = 0;
	Handler h = new Handler();
	boolean already_queried = false;

	Menu menu;

	String selectedLanguage = "TR";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);

		words.clear();
		wordsAfterSearch.clear();

		wordsList = (ListView) findViewById(R.id.listViewMain);
		wordsList.setFastScrollEnabled(true);
		searchText = (EditText) findViewById(R.id.editText1);
		myDBHelper = new SQLiteAssetHelper(this.getApplicationContext());

		try {
			myDBHelper.createDataBase();
		} catch (IOException ioe) {
			throw new Error("Unable to create database");
		}

		try {
			myDBHelper.openDataBase();
		} catch (SQLException sqle) {
			throw sqle;
		}

		WordAdapter.stringLanguage = selectedLanguage;
		words = showList();
		final WordAdapter wordAdapter = new WordAdapter(SearchActivity.this,
				words, selectedLanguage);
		wordsList.setAdapter(wordAdapter);
		wordsBosnian = showList();
		Collections.sort(wordsBosnian, new Word.OrderByBosnian());
		wordsEnglish = showList();
		Collections.sort(wordsEnglish, new Word.OrderByEnglish());

		searchText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				// Ignore if don't need
			}

			@Override
			public void afterTextChanged(Editable arg0) {

				last_text_edit = System.currentTimeMillis();
				h.postDelayed(input_finish_checker, idle_min);

			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
			}
		});

		wordsList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Toast.makeText(getApplicationContext(),
						((TextView) view).getText(), Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void query_dictionary_after_text_changed() {

		if (selectedLanguage.equals("TR")) {
			if (searchText.getText().toString().length() == 0) {
				final WordAdapter wordAdapter = new WordAdapter(
						SearchActivity.this, words, selectedLanguage);
				wordsList.setAdapter(wordAdapter);
				wordsList.setFastScrollEnabled(true);
			} else
				wordsAfterSearch = showListAfterSearchPerformed();
		} else if (selectedLanguage.equals("BS")) {
			if (searchText.getText().toString().length() == 0) {
				final WordAdapter wordAdapterBosnian = new WordAdapter(
						SearchActivity.this, wordsBosnian, selectedLanguage);
				wordsList.setAdapter(wordAdapterBosnian);
				wordsList.setFastScrollEnabled(true);
			} else {
				wordsAfterSearch = showListAfterSearchPerformed();
				Collections.sort(wordsAfterSearch, new Word.OrderByBosnian());
			}
		} else if (selectedLanguage.equals("EN")) {
			if (searchText.getText().toString().length() == 0) {
				final WordAdapter wordAdapterEnglish = new WordAdapter(
						SearchActivity.this, wordsEnglish, selectedLanguage);
				wordsList.setAdapter(wordAdapterEnglish);
				wordsList.setFastScrollEnabled(true);
			} else {
				wordsAfterSearch = showListAfterSearchPerformed();
				Collections.sort(wordsAfterSearch, new Word.OrderByEnglish());
			}
		}

		if (searchText.getText().toString().length() != 0) {
			wordsList.setFastScrollEnabled(false);
			final WordAdapter wordAdapterSearch = new WordAdapter(
					SearchActivity.this, wordsAfterSearch, selectedLanguage);
			wordsList.setAdapter(wordAdapterSearch);
		}

		already_queried = false;
	}

	public ArrayList<Word> showList() {
		ArrayList<Word> words = new ArrayList<Word>();
		words.clear();
		Cursor c = myDBHelper.getAllWords();
		String tr = null, bs = null, en = null, tr_temp = null, bs_temp, en_temp, bs_P = null, tr_P = null;
		for (int ID = 0; ID < c.getCount(); ID = ID + 1) {
			if (ID == 0) {
				if (c.moveToFirst()) {
					tr_temp = tr;
					tr = c.getString(c.getColumnIndex("TURKISH"));
					bs_temp = bs;
					bs = c.getString(c.getColumnIndex("BOSNIAN"));
					if (bs == null)
						bs = bs_temp;
					en_temp = en;
					en = c.getString(c.getColumnIndex("ENGLISH"));
					if (en == null)
						en = en_temp;
					bs_P = c.getString(c.getColumnIndex("BOSNIAN_RONUNCIATION"));
					tr_P = c.getString(c
							.getColumnIndex("TURKISH_PRONUNCIATION"));
				} else {
					tr = null;
					bs = null;
					bs_temp = null;
					en = null;
					en_temp = null;
				}
				try {
					if (!tr.equals(tr_temp)) {
						Word word = new Word(tr, bs, en, " [" + bs_P + "]",
								" [" + tr_P + "]", 0);
						words.add(word);
					}
				} catch (NullPointerException e) {
					Word word = new Word(tr, bs, en, " [" + bs_P + "]", " ["
							+ tr_P + "]", 0);
					words.add(word);
				}

			}

			else {
				if (c.moveToNext()) {
					tr_temp = tr;
					tr = c.getString(c.getColumnIndex("TURKISH"));
					bs_temp = bs;
					bs = c.getString(c.getColumnIndex("BOSNIAN"));
					if (bs == null)
						bs = bs_temp;
					en_temp = en;
					en = c.getString(c.getColumnIndex("ENGLISH"));
					if (en == null)
						en = en_temp;
					bs_P = c.getString(c.getColumnIndex("BOSNIAN_RONUNCIATION"));
					tr_P = c.getString(c
							.getColumnIndex("TURKISH_PRONUNCIATION"));
				} else {
					tr = null;
					bs = null;
					bs_temp = null;
					en = null;
					en_temp = null;
				}

				try {
					if (!tr.equals(tr_temp)) {
						Word word = new Word(tr, bs, en, " [" + bs_P + "]",
								" [" + tr_P + "]", 0);
						words.add(word);
					}
				} catch (NullPointerException e) {
					Word word = new Word(tr, bs, en, " [" + bs_P + "]", " ["
							+ tr_P + "]", 0);
					words.add(word);
				}

			}
		}
		return words;
	}

	public ArrayList<Word> showListAfterSearchPerformed() {
		ArrayList<Word> words = new ArrayList<Word>();
		words.clear();
		Cursor c1;

		String tr = null, bs = null, en = null, tr_temp = null, bs_P = null, tr_P = null;

		if (selectedLanguage.equals("TR")) {
			String searchString = searchText.getText().toString();
			String newSearchString = searchString.replace("i", "İ");
			c1 = myDBHelper.getSearchTurkish(newSearchString.toUpperCase()); // This
																				// code
																				// works,
																				// ignore
																				// warning
																				// about
																				// locale
			c1.moveToFirst();

			for (int ID = 0; ID < c1.getCount(); ID = ID + 1) {
				if (ID == 0) {
					if (c1.moveToFirst()) {
						tr_temp = tr;
						tr = c1.getString(c1.getColumnIndex("TURKISH"));
					} else {
						tr = null;
						tr_temp = null;
					}
				} else {
					if (c1.moveToNext()) {
						tr_temp = tr;
						tr = c1.getString(c1.getColumnIndex("TURKISH"));
					} else {
						tr = null;
						tr_temp = null;
					}
				}
				bs = c1.getString(c1.getColumnIndex("BOSNIAN"));
				en = c1.getString(c1.getColumnIndex("ENGLISH"));
				bs_P = c1.getString(c1.getColumnIndex("BOSNIAN_RONUNCIATION"));
				tr_P = c1.getString(c1.getColumnIndex("TURKISH_PRONUNCIATION"));

				try {
					if (!tr.equals(tr_temp)) {
						Word word = new Word(tr, bs, en, " [" + bs_P + "]",
								" [" + tr_P + "]", 0);
						words.add(word);
					}
				} catch (NullPointerException e) {
					Word word = new Word(tr, bs, en, " [" + bs_P + "]", " ["
							+ tr_P + "]", 0);
					words.add(word);
				}
			}
			c1.close();
		}

		else if (selectedLanguage.equals("BS")) {
			c1 = myDBHelper.getSearchBosnian(searchText.getText().toString()
					.toUpperCase()); // This code works, ignore warning about
										// locale
			c1.moveToFirst();

			for (int ID = 0; ID < c1.getCount(); ID = ID + 1) {
				if (ID == 0) {
					if (c1.moveToFirst()) {
						tr_temp = tr;
						tr = c1.getString(c1.getColumnIndex("TURKISH"));
					} else {
						tr = null;
						tr_temp = null;
					}
				} else {
					if (c1.moveToNext()) {
						tr_temp = tr;
						tr = c1.getString(c1.getColumnIndex("TURKISH"));
					} else {
						tr = null;
						tr_temp = null;
					}
				}
				bs = c1.getString(c1.getColumnIndex("BOSNIAN"));
				en = c1.getString(c1.getColumnIndex("ENGLISH"));
				bs_P = c1.getString(c1.getColumnIndex("BOSNIAN_RONUNCIATION"));
				tr_P = c1.getString(c1.getColumnIndex("TURKISH_PRONUNCIATION"));
				try {
					if (!tr.equals(tr_temp)) {
						Word word = new Word(tr, bs, en, " [" + bs_P + "]",
								" [" + tr_P + "]", 0);
						words.add(word);
					}
				} catch (NullPointerException e) {
					Word word = new Word(tr, bs, en, " [" + bs_P + "]", " ["
							+ tr_P + "]", 0);
					words.add(word);
				}
			}
			c1.close();
		}

		else if (selectedLanguage.equals("EN")) {
			c1 = myDBHelper.getSearchEnglish(searchText.getText().toString()
					.toUpperCase()); // This code works, ignore warning about
										// locale
			c1.moveToFirst();

			for (int ID = 0; ID < c1.getCount(); ID = ID + 1) {
				if (ID == 0) {
					if (c1.moveToFirst()) {
						tr_temp = tr;
						tr = c1.getString(c1.getColumnIndex("TURKISH"));
					} else {
						tr = null;
						tr_temp = null;
					}
				} else {
					if (c1.moveToNext()) {
						tr_temp = tr;
						tr = c1.getString(c1.getColumnIndex("TURKISH"));
					} else {
						tr = null;
						tr_temp = null;
					}
				}
				bs = c1.getString(c1.getColumnIndex("BOSNIAN"));
				en = c1.getString(c1.getColumnIndex("ENGLISH"));
				bs_P = c1.getString(c1.getColumnIndex("BOSNIAN_RONUNCIATION"));
				tr_P = c1.getString(c1.getColumnIndex("TURKISH_PRONUNCIATION"));

				try {
					if (!tr.equals(tr_temp)) {
						Word word = new Word(tr, bs, en, " [" + bs_P + "]",
								" [" + tr_P + "]", 0);
						words.add(word);
					}
				} catch (NullPointerException e) {
					Word word = new Word(tr, bs, en, " [" + bs_P + "]", " ["
							+ tr_P + "]", 0);
					words.add(word);
				}
			}
			c1.close();
		}
		return words;
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		getSupportMenuInflater().inflate(R.menu.search, menu);
		this.menu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {

		com.actionbarsherlock.view.MenuItem languageItem = menu
				.findItem(R.id.menu_language);

		switch (item.getItemId()) {
		case R.id.action_about:
			showAbout();
			break;
		case R.id.languageTurkish:
			switchToTurkish();
			languageItem.setIcon(R.drawable.ic_action_tr);
			break;
		case R.id.languageBosnian:
			switchToBosnian();
			languageItem.setIcon(R.drawable.ic_action_bs);
			break;
		case R.id.languageEnglish:
			switchToEnglish();
			languageItem.setIcon(R.drawable.ic_action_en);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void switchToTurkish() {
		selectedLanguage = "TR";

		WordAdapter.stringLanguage = "TR";

		if (searchText.getText().toString().length() == 0) {
			final WordAdapter wordAdapter = new WordAdapter(
					SearchActivity.this, words, selectedLanguage);
			wordsList.setAdapter(wordAdapter);
			wordsList.setFastScrollEnabled(false);
			wordsList.setFastScrollEnabled(true);
		} else {
			wordsList.setFastScrollEnabled(false);
			final WordAdapter wordAdapterSearch = new WordAdapter(
					SearchActivity.this, showListAfterSearchPerformed(),
					selectedLanguage);
			wordsList.setAdapter(wordAdapterSearch);
		}
	}

	public void switchToBosnian() {
		selectedLanguage = "BS";

		WordAdapter.stringLanguage = "BS";
		Log.d("Sortion of section letters", "Static variable updated to: "
				+ WordAdapter.stringLanguage);
		if (searchText.getText().toString().length() == 0) {
			final WordAdapter wordAdapterBosnian = new WordAdapter(
					SearchActivity.this, wordsBosnian, selectedLanguage);
			wordsList.setAdapter(wordAdapterBosnian);
			wordsList.setFastScrollEnabled(false);
			wordsList.setFastScrollEnabled(true);
		} else {
			wordsList.setFastScrollEnabled(false);
			wordsAfterSearch = showListAfterSearchPerformed();
			Collections.sort(wordsAfterSearch, new Word.OrderByBosnian());
			final WordAdapter wordAdapterSearch = new WordAdapter(
					SearchActivity.this, wordsAfterSearch, selectedLanguage);
			wordsList.setAdapter(wordAdapterSearch);
		}
	}

	public void switchToEnglish() {
		selectedLanguage = "EN";

		WordAdapter.stringLanguage = "EN";
		Log.d("Sortion of section letters", "Static variable updated to: "
				+ WordAdapter.stringLanguage);
		if (searchText.getText().toString().length() == 0) {
			final WordAdapter wordAdapterEnglish = new WordAdapter(
					SearchActivity.this, wordsEnglish, selectedLanguage);
			wordsList.setAdapter(wordAdapterEnglish);
			wordsList.setFastScrollEnabled(false);
			wordsList.setFastScrollEnabled(true);
		} else {
			wordsList.setFastScrollEnabled(false);
			wordsAfterSearch = showListAfterSearchPerformed();
			Collections.sort(wordsAfterSearch, new Word.OrderByEnglish());
			final WordAdapter wordAdapterSearch = new WordAdapter(
					SearchActivity.this, wordsAfterSearch, selectedLanguage);
			wordsList.setAdapter(wordAdapterSearch);
		}
	}

	public void showAbout() {
		// Create custom dialog object
		final Dialog dialog = new Dialog(SearchActivity.this);
		// Include dialog.xml file
		dialog.setContentView(R.layout.about);
		// Set dialog title
		dialog.setTitle(getString(R.string.action_about));

		// set values for custom dialog components - text, image and button
		ImageView image = (ImageView) dialog.findViewById(R.id.imageDialog);
		image.setImageResource(R.drawable.logo);

		dialog.show();
	}

	private Runnable input_finish_checker = new Runnable() {

		@Override
		public void run() {
			if (System.currentTimeMillis() > (last_text_edit + idle_min - 500)) {
				// user hasn't changed the EditText for longer than the minimum
				// delay (with half second buffer window)
				if (!already_queried) {
					already_queried = true;
					query_dictionary_after_text_changed();
				}
			}
		}
	};

}
