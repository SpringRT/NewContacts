package com.a11.contacts;

import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.a11.contacts.fragments.BaseFragment;
import com.a11.contacts.fragments.FragmentData;
import com.a11.contacts.fragments.FragmentDeleted;
import com.a11.contacts.fragments.FragmentList;
import com.a11.contacts.models.Contact;

public class MainActivity extends AppCompatActivity {

    private boolean isPortrait = true;
    private BaseFragment currentFragment;
    private List<Contact> contactList;
    private Set<String> removed;
    private int position;
    private String id;
    private boolean deleteList = false;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isPortrait = getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        getRemoved();
        getContacts();
        setContentView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && isPortrait) {
            isPortrait = false;
            setContentView();
        }
        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && !isPortrait) {
            isPortrait = true;
            setContentView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveRemoved();
    }

    @Override
    public void onBackPressed() {
        if ((isPortrait && currentFragment instanceof FragmentList) ||
                (!isPortrait && !deleteList)) {
            super.onBackPressed();
        } else {
            if (deleteList) {
                deleteList = false;
                fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_delete));

                if (!isPortrait) {
                    FragmentManager fm = getFragmentManager();
                    findViewById(R.id.data_frame).setVisibility(View.VISIBLE);
                    fm.beginTransaction().replace(R.id.contacts_frame, new FragmentList()).commit();
                }
            }
            if (isPortrait) {
                currentFragment = new FragmentList();
                setFragment();
            }
        }
    }

    private void saveRemoved() {
        SharedPreferences prefs = getSharedPreferences("contacts", MODE_PRIVATE);
        prefs.edit().remove("removed").apply();
        prefs.edit().putStringSet("removed", removed).apply();
    }

    private void getRemoved() {
        SharedPreferences prefs = getSharedPreferences("contacts", MODE_PRIVATE);
        removed = prefs.getStringSet("removed", new HashSet<String>());
    }

    private void getContacts() {
        contactList = new ArrayList<>();

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if (cur != null && cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));

                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(

                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                            new String[]{id}, null);
                    if(pCur != null) {
                        while (pCur.moveToNext()) {
                            String phoneNo = pCur.getString(pCur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));

                            Contact contact = new Contact(id, name, phoneNo);
                            boolean b = false;
                            if(removed.contains(contact.getId())) {
                                contact.setDeleted(true);
                            }

                            for(Contact c : contactList) {
                                if(c.getId().equals(id)) {
                                    b = true;
                                    break;
                                }
                            }

                            if(!b) {
                                contactList.add(contact);
                            }
                        }

                        pCur.close();
                    }
                }
            }

            cur.close();
        }
    }

    public void setDeleted(String id) {
        for (int i = 0; i < contactList.size(); i++) {
            if (contactList.get(i).getId().equals(id)) {
                removed.add(id);
                contactList.get(i).setDeleted(true);

                if(isPortrait){
                    currentFragment = new FragmentList();
                    setFragment();
                }
                else{
                    FragmentManager fm = getFragmentManager();
                    fm.beginTransaction().replace(R.id.contacts_frame, new FragmentList()).commit();
                    fm.beginTransaction().replace(R.id.data_frame, new FragmentData()).commit();
                }
                Toast.makeText(this, contactList.get(i).getName()
                        +  " удален!", Toast.LENGTH_LONG).show();

                break;

            }
        }
    }


    private void setContentView() {
        setContentView(R.layout.activity_base);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPortrait) {
                    if(deleteList) {
                        currentFragment = new FragmentList();
                        setFragment();
                        deleteList = false;
                        fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_delete));
                    }
                    else {
                        if (currentFragment instanceof FragmentList) {
                            currentFragment = new FragmentDeleted();
                            deleteList = true;
                            fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_rotate));
                            setFragment();
                        } else if (currentFragment instanceof FragmentData) {
                            setDeleted(id);
                        }
                    }
                }
                else {
                    if(deleteList) {
                        deleteList = false;
                        fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_delete));

                        FragmentManager fm = getFragmentManager();
                        findViewById(R.id.data_frame).setVisibility(View.VISIBLE);
                        fm.beginTransaction().replace(R.id.contacts_frame, new FragmentList()).commit();
                    }
                    else {
                        FragmentManager fm = getFragmentManager();
                        deleteList = true;
                        fm.beginTransaction().replace(R.id.contacts_frame, new FragmentDeleted()).commit();
                        findViewById(R.id.data_frame).setVisibility(View.INVISIBLE);
                        fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_rotate));
                    }
                }
            }
        });

        if(isPortrait) {
            currentFragment = new FragmentList();
            setFragment();
        }
        else {
            FragmentManager fm = getFragmentManager();
            fm.beginTransaction().replace(R.id.contacts_frame, new FragmentList()).commit();
            fm.beginTransaction().replace(R.id.data_frame, new FragmentData()).commit();
        }
    }

    private void setFragment() {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.main_frame, currentFragment).commit();
    }

    public List<Contact> getContactList() {
        return contactList;
    }

    public void sendMessage(String number) {
        Intent sms = new Intent();
        sms.setAction(Intent.ACTION_VIEW);
        sms.setData(Uri.fromParts("sms", number, null));

        startActivity(sms);
    }

    public void sendData(Bundle data) {
        id = data.getString("id", "");
        for (int i = 0; i < contactList.size(); i++) {
            if(contactList.get(i).getId().equals(id)) {
                position = i;
                break;
            }
        }

        if(position != -1 && !id.isEmpty()) {
            Bundle fData = new Bundle();
            fData.putString("id", contactList.get(position).getId());
            fData.putString("name", contactList.get(position).getName());
            fData.putString("phone", contactList.get(position).getPhoneNumber());

            if(isPortrait) {
                currentFragment = new FragmentData();
                currentFragment.setArguments(fData);
                setFragment();
            }
            else {
                FragmentData fragment = new FragmentData();
                fragment.setArguments(fData);
                getFragmentManager().beginTransaction().
                        replace(R.id.data_frame, fragment).commit();
            }
        }
    }

}
