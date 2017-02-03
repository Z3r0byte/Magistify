/*
 * Copyright (c) 2016-2017 Bas van den Boom 'Z3r0byte'
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.z3r0byte.magistify;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.z3r0byte.magistify.Util.ConfigUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class DonationActivity extends AppCompatActivity {
    private static final String TAG = "DonationActivity";

    Toolbar mToolbar;

    final static String SKU_FIFTY_CENTS = "fifty_cents";
    final static String SKU_ONE_EURO = "one_euro";
    final static String SKU_TWO_EURO = "two_euro";
    final static String SKU_FIVE_EURO = "five_euro";

    ConfigUtil configUtil;

    IInAppBillingService mService;
    Bundle ownedItems;
    ArrayList<String> boughtSKU = new ArrayList<>();
    ArrayList<String> boughtToken = new ArrayList<>();

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
        }
    };
    Bundle querySkus = new Bundle();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);

        mToolbar = (Toolbar) findViewById(R.id.Toolbar);
        mToolbar.setTitle(R.string.title_donate);
        mToolbar.setNavigationIcon(R.drawable.back);
        setSupportActionBar(mToolbar);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        configUtil = new ConfigUtil(this);


        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);

        ArrayList<String> skuList = new ArrayList<String>();
        skuList.add(SKU_FIFTY_CENTS);
        skuList.add(SKU_ONE_EURO);
        skuList.add(SKU_TWO_EURO);
        skuList.add(SKU_FIVE_EURO);
        querySkus.putStringArrayList("ITEM_ID_LIST", skuList);


        Button fiftyCents = (Button) findViewById(R.id.fifty_cents_button);
        fiftyCents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                purchase(SKU_FIFTY_CENTS);
            }
        });

        Button oneEuro = (Button) findViewById(R.id.one_euro_button);
        oneEuro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                purchase(SKU_ONE_EURO);
            }
        });

        Button twoEuro = (Button) findViewById(R.id.two_euro_button);
        twoEuro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                purchase(SKU_TWO_EURO);
            }
        });

        Button fiveEuro = (Button) findViewById(R.id.five_euro_button);
        fiveEuro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                purchase(SKU_FIVE_EURO);
            }
        });

        getPurchases();
    }

    private void getPurchases() {
        final ProgressDialog dialog = ProgressDialog.show(this, "",
                getString(R.string.msg_loading_purchases), true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                boughtSKU.clear();
                boughtToken.clear();
                try {
                    Thread.sleep(500);
                    ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);

                    int response = ownedItems.getInt("RESPONSE_CODE");
                    if (response == 0) {
                        ArrayList<String> ownedSkus =
                                ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                        ArrayList<String> purchaseDataList =
                                ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                        ArrayList<String> signatureList =
                                ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");

                        configUtil.setBoolean("disable_ads", false);
                        configUtil.setBoolean("pro_unlocked", false);

                        for (int i = 0; i < purchaseDataList.size(); ++i) {
                            String purchaseData = purchaseDataList.get(i);
                            String signature = signatureList.get(i);
                            String sku = ownedSkus.get(i);

                            JSONObject jo = new JSONObject(purchaseData);
                            String token = jo.getString("purchaseToken");

                            boughtSKU.add(sku);
                            boughtToken.add(token);

                            Log.i(TAG, "run: Purchased item " + i + ": SKU: " + sku +
                                    ", purchaseData:" + purchaseData + ", Signature: " + signature);

                            if (boughtSKU.contains(SKU_FIFTY_CENTS)) {
                                configUtil.setBoolean("disable_ads", true);
                            } else if (boughtSKU.contains(SKU_ONE_EURO)) {
                                configUtil.setBoolean("disable_ads", true);
                                configUtil.setBoolean("pro_unlocked", true);
                                configUtil.setString("token_one_euro", token);
                            } else if (boughtSKU.contains(SKU_TWO_EURO)) {
                                configUtil.setBoolean("disable_ads", true);
                                configUtil.setBoolean("pro_unlocked", true);
                                configUtil.setString("token_two_euro", token);
                            } else if (boughtSKU.contains(SKU_FIVE_EURO)) {
                                configUtil.setBoolean("disable_ads", true);
                                configUtil.setBoolean("pro_unlocked", true);
                                configUtil.setString("token_five_euro", token);
                            }

                            // do something with this purchase information
                            // e.g. display the updated list of products owned by user
                        }
                    }

                } catch (RemoteException e) {
                    if (mService != null) {
                        unbindService(mServiceConn);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DonationActivity.this, R.string.err_no_connection, Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                    finish();
                } catch (InterruptedException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DonationActivity.this, R.string.err_unknown, Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                    finish();
                } catch (JSONException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DonationActivity.this, R.string.err_unknown, Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                    finish();
                }

                dialog.dismiss();
            }
        }).start();
    }

    private void purchase(final String SKU) {
        if (!boughtSKU.contains(SKU)) {
            Log.i(TAG, "purchase: Starting purchase flow...");
            try {
                Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                        SKU, "inapp", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
                PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                startIntentSenderForResult(pendingIntent.getIntentSender(),
                        1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                        Integer.valueOf(0));
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(getString(R.string.dialog_item_purchased_title));
            alertDialogBuilder.setMessage(getString(R.string.dialog_item_purchased_desc));
            alertDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    consumePurchase(SKU);
                }
            });
            alertDialogBuilder.setNegativeButton("Annuleren", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    private void consumePurchase(final String SKU) {
        final String token = boughtToken.get(boughtSKU.indexOf(SKU));
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                try {
                    Log.i(TAG, "run: Consuming Purchase with SKU;TOKEN " + SKU + ";" + token);
                    mService.consumePurchase(3, getPackageName(), token);
                    Thread.sleep(500);
                    getPurchases();
                    Thread.sleep(200);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            purchase(SKU);
                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                String boughtSKU;
                String token;
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    boughtSKU = jo.getString("productId");
                    token = jo.getString("purchaseToken");
                } catch (JSONException e) {
                    Toast.makeText(this, R.string.err_purchase_unkown, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    return;
                }

                Log.i(TAG, "onActivityResult: SKU: " + boughtSKU);

                if (boughtSKU.equals(SKU_FIFTY_CENTS)) {
                    configUtil.setBoolean("disable_ads", true);
                } else if (boughtSKU.equals(SKU_ONE_EURO)) {
                    configUtil.setBoolean("disable_ads", true);
                    configUtil.setBoolean("pro_unlocked", true);
                    configUtil.setString("token_one_euro", token);
                } else if (boughtSKU.equals(SKU_TWO_EURO)) {
                    configUtil.setBoolean("disable_ads", true);
                    configUtil.setBoolean("pro_unlocked", true);
                    configUtil.setString("token_two_euro", token);
                } else if (boughtSKU.equals(SKU_FIVE_EURO)) {
                    configUtil.setBoolean("disable_ads", true);
                    configUtil.setBoolean("pro_unlocked", true);
                    configUtil.setString("token_five_euro", token);
                }

                Toast.makeText(this, R.string.msg_purchase_successful, Toast.LENGTH_SHORT).show();

                getPurchases();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, R.string.err_purchase_cancalled, Toast.LENGTH_LONG).show();
                getPurchases();
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            unbindService(mServiceConn);
        }
    }
}
