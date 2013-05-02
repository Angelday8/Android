/*
 * Copyright 2011-2012 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package piuk.blockchain.android.ui;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.WrongNetworkException;
import com.google.bitcoin.uri.BitcoinURIParseException;

import piuk.BitcoinAddress;
import piuk.BitcoinURI;
import piuk.MyRemoteWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.WalletApplication.AddAddressCallback;
import piuk.blockchain.android.ui.dialogs.RequestPasswordDialog;
import piuk.blockchain.android.util.ActionBarFragment;
import piuk.blockchain.android.util.ViewPagerTabs;

/**
 * @author Andreas Schildbach
 */
public final class WalletAddressesActivity extends AbstractWalletActivity {
	public static void start(final Context context, final boolean sending) {
		final Intent intent = new Intent(context, WalletAddressesActivity.class);
		intent.putExtra(EXTRA_SENDING, sending);
		context.startActivity(intent);
	}

	private static final String EXTRA_SENDING = "sending";

	private static final int REQUEST_CODE_SCAN = 0;

	private WalletActiveAddressesFragment activeAddressesFragment;
	private WalletArchivedAddressesFragment archivedAddressesFragment;
	private SendingAddressesFragment sendingAddressesFragment;
	private ImageButton addButton;
	private ImageButton pasteButton;
	private ImageButton scanButton;

	private final Handler handler = new Handler();


	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.address_book_content);

		final ActionBarFragment actionBar = getActionBarFragment();

		actionBar.setPrimaryTitle(R.string.address_book_activity_title);

		actionBar.setBack(new OnClickListener() {
			public void onClick(final View v) {
				finish();
			}
		});

		final ViewPager pager = (ViewPager) findViewById(R.id.address_book_pager);

		if (pager != null) {
			final ViewPagerTabs pagerTabs = (ViewPagerTabs) findViewById(R.id.address_book_pager_tabs);
			pagerTabs.addTabLabels(R.string.address_book_list_receiving_title,
					R.string.address_book_list_archived_title,
					R.string.address_book_list_sending_title);
			
			final ProxyOnPageChangeListener pagerListener = new ProxyOnPageChangeListener(
					pagerTabs) {
				@Override
				public void onPageSelected(final int position) {

					super.onPageSelected(position);

					if (position == 0) {
						if (scanButton != null) {
							actionBar.removeButton(scanButton);
							scanButton = null;
						}

						if (pasteButton != null) {
							actionBar.removeButton(pasteButton);
							pasteButton = null;
						}

						if (addButton == null) {
							addButton = actionBar
									.addButton(R.drawable.ic_action_add);
							addButton
									.setOnClickListener(addAddressClickListener);
						}
					} else if (position == 1) {
						if (scanButton != null) {
							actionBar.removeButton(scanButton);
							scanButton = null;
						}

						if (pasteButton != null) {
							actionBar.removeButton(pasteButton);
							pasteButton = null;
						}

						if (addButton == null) {
							actionBar.removeButton(addButton);
							addButton = null;
						}
					} else if (position == 2) {
						if (addButton != null) {
							actionBar.removeButton(addButton);
							addButton = null;
						}

						if (scanButton == null) {
							scanButton = actionBar
									.addButton(R.drawable.ic_action_qr);
							scanButton.setOnClickListener(scanClickListener);
						}

						if (pasteButton == null) {
							pasteButton = actionBar
									.addButton(R.drawable.ic_action_paste);
							pasteButton
									.setOnClickListener(pasteClipboardClickListener);
						}
					}
				}
			};

			final PagerAdapter pagerAdapter = new PagerAdapter(
					getSupportFragmentManager());

			pager.getCurrentItem();

			pager.setAdapter(pagerAdapter);
			pager.setOnPageChangeListener(pagerListener);
			final int position = getIntent().getBooleanExtra(EXTRA_SENDING,
					true) == true ? 2 : 0;
			pager.setCurrentItem(position);
			pager.setPageMargin(2);
			pager.setPageMarginDrawable(R.color.background_less_bright);

			pagerListener.onPageSelected(position);
			pagerListener.onPageScrolled(position, 0, 0);

			archivedAddressesFragment = new WalletArchivedAddressesFragment(2,
					pager);
			activeAddressesFragment = new WalletActiveAddressesFragment(0,
					pager);
			sendingAddressesFragment = new SendingAddressesFragment();
		}
		/*
		 * else { scanButton = actionBar.addButton(R.drawable.ic_action_qr);
		 * scanButton.setOnClickListener(scanClickListener);
		 * 
		 * pasteButton = actionBar.addButton(R.drawable.ic_action_paste);
		 * pasteButton.setOnClickListener(pasteClipboardClickListener);
		 * 
		 * addButton = actionBar.addButton(R.drawable.ic_action_add);
		 * addButton.setOnClickListener(addAddressClickListener);
		 * 
		 * activeAddressesFragment = (WalletActiveAddressesFragment)
		 * getSupportFragmentManager
		 * ().findFragmentById(R.id.wallet_addresses_fragment);
		 * archivedAddressesFragment = (WalletArchivedAddressesFragment)
		 * getSupportFragmentManager
		 * ().findFragmentById(R.id.wallet_archived_fragment);
		 * sendingAddressesFragment = (SendingAddressesFragment)
		 * getSupportFragmentManager
		 * ().findFragmentById(R.id.sending_addresses_fragment); }
		 */

		updateFragments();
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode,
			final Intent intent) {
		if (requestCode == REQUEST_CODE_SCAN
				&& resultCode == RESULT_OK
				&& "QR_CODE"
						.equals(intent.getStringExtra("SCAN_RESULT_FORMAT"))) {
			final String contents = intent.getStringExtra("SCAN_RESULT");

			try {
				final BitcoinAddress address;

				if (contents.matches("[a-zA-Z0-9]*")) {
					address = new BitcoinAddress(contents);
				} else {
					final BitcoinURI bitcoinUri = new BitcoinURI(contents);
					address = bitcoinUri.getAddress();
				}

				handler.postDelayed(new Runnable() {
					public void run() {
						EditAddressBookEntryFragment.edit(
								getSupportFragmentManager(), address.toString());
					}
				}, 500);
			} catch (final AddressFormatException x) {
				errorDialog(R.string.send_coins_uri_parse_error_title, contents);
			} catch (final BitcoinURIParseException x) {
				errorDialog(R.string.send_coins_uri_parse_error_title, contents);
			}
		}
	}

	private void updateFragments() {

		if (application.getRemoteWallet() == null)
			return;
		
		final String[] addressesArray = application.getRemoteWallet().getActiveAddresses();
		final ArrayList<Address> addresses = new ArrayList<Address>(addressesArray.length);

		for (final String address : addressesArray) {
			try {
				addresses.add(new Address(Constants.NETWORK_PARAMETERS, address));
			} catch (WrongNetworkException e) {
				e.printStackTrace();
			} catch (AddressFormatException e) {
				e.printStackTrace();
			}
		}

		sendingAddressesFragment.setWalletAddresses(addresses);
	}

	private class PagerAdapter extends FragmentStatePagerAdapter {
		public PagerAdapter(final FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public Fragment getItem(final int position) {
			if (position == 0)
				return activeAddressesFragment;
			else if (position == 1)
				return archivedAddressesFragment;
			else
				return sendingAddressesFragment;
		}
	}

	private final OnClickListener scanClickListener = new OnClickListener() {
		public void onClick(final View v) {
			handleScan();
		}
	};

	private void handleScan() {
		if (getPackageManager().resolveActivity(Constants.INTENT_QR_SCANNER, 0) != null) {
			startActivityForResult(Constants.INTENT_QR_SCANNER,
					REQUEST_CODE_SCAN);
		} else {
			showMarketPage(Constants.PACKAGE_NAME_ZXING);
			longToast(R.string.send_coins_install_qr_scanner_msg);
		}
	}

	private final OnClickListener pasteClipboardClickListener = new OnClickListener() {
		public void onClick(final View v) {
			handlePasteClipboard();
		}
	};

	private void handlePasteClipboard() {
		final ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

		if (clipboardManager.hasText()) {
			final String text = clipboardManager.getText().toString().trim();

			try {
				final Address address = new Address(
						Constants.NETWORK_PARAMETERS, text);
				EditAddressBookEntryFragment.edit(getSupportFragmentManager(),
						address.toString());
			} catch (final AddressFormatException x) {
				toast(R.string.send_coins_parse_address_error_msg);
			}
		} else {
			toast(R.string.address_book_msg_clipboard_empty);
		}
	}

	private final OnClickListener addAddressClickListener = new OnClickListener() {
		public void onClick(final View v) {
			handleAddAddress();
		}
	};

	private void reallyGenerateAddress() {
		application.addKeyToWallet(new ECKey(), null, 0,
				new AddAddressCallback() {

					public void onSavedAddress(String address) {
						System.out.println("Generated Address " + address);

						updateFragments();
					}

					public void onError() {
						System.out.println("Generate Address Failed");

						updateFragments();
					}
				});
	}

	private void handleAddAddress() {
		new AlertDialog.Builder(WalletAddressesActivity.this)
				.setTitle(R.string.wallet_addresses_fragment_add_dialog_title)
				.setMessage(
						R.string.wallet_addresses_fragment_add_dialog_message)
				.setPositiveButton(
						R.string.wallet_addresses_fragment_add_dialog_positive,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int which) {

								if (application.getRemoteWallet() == null)
									return;
								
								MyRemoteWallet remoteWallet = application.getRemoteWallet();

								if (remoteWallet.isDoubleEncrypted() == false) {
									System.out.println("Not double encrypted");

									reallyGenerateAddress();
								} else {
									if (remoteWallet.temporySecondPassword == null) {
										RequestPasswordDialog.show(
												getSupportFragmentManager(),
												new SuccessCallback() {

													public void onSuccess() {
														reallyGenerateAddress();
													}

													public void onFail() {
														Toast.makeText(
																getApplication(),
																R.string.generate_key_no_password_error,
																Toast.LENGTH_LONG)
																.show();
													}
												}, RequestPasswordDialog.PasswordTypeSecond);
									} else {
										System.out.println("Password Set");

										reallyGenerateAddress();
									}
								}

								updateFragments();
							}
						}).setNegativeButton(R.string.button_cancel, null)
				.show();
	}

	private class ProxyOnPageChangeListener implements OnPageChangeListener {
		private final OnPageChangeListener onPageChangeListener;

		public ProxyOnPageChangeListener(
				final OnPageChangeListener onPageChangeListener) {
			this.onPageChangeListener = onPageChangeListener;
		}

		public void onPageScrolled(final int position,
				final float positionOffset, final int positionOffsetPixels) {
			onPageChangeListener.onPageScrolled(position, positionOffset,
					positionOffsetPixels);
		}

		public void onPageSelected(final int position) {
			onPageChangeListener.onPageSelected(position);
		}

		public void onPageScrollStateChanged(final int state) {
			onPageChangeListener.onPageScrollStateChanged(state);
		}
	}
}
