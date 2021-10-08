package com.example.contacts;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;

import android.view.View;
import android.view.ViewGroup;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.contacts.Interface.IActionContacts;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyPermission;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String FILE_NAME="contacts.csv";
    private static String FROM ="FROM-DEVICE";
    ContactsAdapterCustom myAdapter;
    ArrayList<Contact> listContacts;
    ArrayList<Contact> listContactsToDelete;
    RecyclerView rv_contacts;
    IActionContacts action;
    ImageView btn_delete,btn_refresh,btn_readCSV,btn_writeCSV;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prepare();
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
                Load(FROM);
            }
            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(MainActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };
        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();
    }
    private void Load(String s) {
        if(s=="FROM-DEVICE") listContacts=action.getContacts();
        if(s=="FROM-CSV") {
            listContacts=action.getContactsFromCSV();
            if(listContacts.size()==0){
                FROM="FROM-DEVICE";
                listContacts=action.getContacts();
            }
        }
        ContactsAdapterCustom myAdapter = new ContactsAdapterCustom(listContacts,MainActivity.this,action);
        rv_contacts.setAdapter(myAdapter);
    }
    @Override
    protected void onResume() {
        super.onResume();
        ContactsAdapterCustom myAdapter = new ContactsAdapterCustom(listContacts,MainActivity.this,action);
        rv_contacts.setAdapter(myAdapter);
    }
    private void prepare(){
        listContacts = new ArrayList<>();
        listContactsToDelete = new ArrayList<>();
        rv_contacts=(RecyclerView) findViewById(R.id.rv_contacts);
        btn_delete = (ImageView) findViewById(R.id.iv_delete);
        btn_refresh = (ImageView) findViewById(R.id.iv_refresh);
        btn_readCSV = (ImageView) findViewById(R.id.iv_read);
        btn_writeCSV = (ImageView) findViewById(R.id.iv_write);
        btn_delete.setOnClickListener(MainActivity.this);
        btn_refresh.setOnClickListener(MainActivity.this);
        btn_readCSV.setOnClickListener(MainActivity.this);
        btn_writeCSV.setOnClickListener(MainActivity.this);
        if(!checkExternalStorageAvailable()){
            btn_writeCSV.setEnabled(false);
        }
        LinearLayoutManager layout = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rv_contacts.setLayoutManager(layout);
        rv_contacts.setHasFixedSize(true);
        RecyclerView.ItemDecoration decoration = new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        rv_contacts.addItemDecoration(decoration);
        action = new IActionContacts(){
            @Override
            public ArrayList<Contact> getContacts(){
                ArrayList<Contact> contacts = new ArrayList<Contact>();
                Uri uri = ContactsContract.Contacts.CONTENT_URI;
                String sort = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;
                Cursor cursor = getContentResolver().query(uri, null , null , null , sort);
                if(cursor.getCount() >0) {
                    while (cursor.moveToNext()){
                        String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        Uri uriPhone = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                        String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID+" =?";
                        Cursor phoneCursor = getContentResolver().query(uriPhone,null,selection,new String[]{id},null);
                        if(phoneCursor.moveToNext()){
                            String number = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            Contact contact = new Contact(name,number);
                            contacts.add(contact);
                            phoneCursor.close();
                        }
                    }
                    cursor.close();
                }
                return contacts;
            }
            @Override
            public ArrayList<Contact> getContactsFromCSV() {
                ArrayList<Contact> contacts = new ArrayList<Contact>();
                String line;
                try {
                        FileInputStream file = openFileInput(FILE_NAME);
                        InputStreamReader  input = new InputStreamReader(file);
                        BufferedReader reader = new BufferedReader(input);
                        reader.readLine();
                        while((line = reader.readLine())!= null) {
                            String[] tokens = line.split(","); // tách line thành từng cột bởi "
                            Contact contact = new Contact(tokens[0]+" (csv)",tokens[1]);
                            contacts.add(contact);
                        }
                        if(contacts.size()>0)  Toast.makeText(MainActivity.this , "Lấy dữ liệu thành công",Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this , "Chưa có dữ liệu từ csv , lưu vào trước khi load  :"+e.toString(),Toast.LENGTH_SHORT).show();
                    }
                    return contacts;
            }
            @Override
            public void saveContactsToCSV(){

                try{
                    //device file explorer : data/data/com.example.contact/files/contacts.csv
                    FileOutputStream output = openFileOutput(FILE_NAME,MODE_PRIVATE);
                    output.write("name,phone\n".getBytes(StandardCharsets.UTF_8));
                    for (Contact contact: listContacts) {
                        String line = String.valueOf(contact.getName()+","+contact.getPhone()+"\n");
                        output.write(line.getBytes(StandardCharsets.UTF_8));
                    }
                    Toast.makeText(MainActivity.this , "Lưu dữ liệu thành công",Toast.LENGTH_SHORT).show();
                }catch (Exception e)
                {
                    Toast.makeText(MainActivity.this , "Lưu dữ liệu thất bại",Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void deleteContact(Context c,Contact contact){
                Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(contact.getPhone()));
                String[] projection = new String[]{
                        ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.Contacts.LOOKUP_KEY
                };
                Cursor cur = c.getContentResolver().query(contactUri , projection , null ,null,null);
                cur.moveToFirst();
                try{
                    String a = cur.getString(cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                    if(cur.getString(cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)).equalsIgnoreCase(contact.getName()));
                    {
                        String lookupKey = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,lookupKey);
                        c.getContentResolver().delete(uri,null,null);
                        Toast.makeText(MainActivity.this , "Deleted", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this , "Errol :"+e.toString(), Toast.LENGTH_SHORT).show();
                }finally{
                    cur.close();
                    cur=null;
                }
            }
            @Override
            public void addContactToRemoveList(Contact contact){
                listContactsToDelete.add(contact);
            }
            @Override
            public void deleteContactFromRemoveList(Contact contact){
                listContactsToDelete.remove(contact);
            }
        };
    }
    @Override
    public void onClick(View view){
            if(view == btn_delete){
                    if(FROM=="FROM-CSV"){
                        Toast.makeText(MainActivity.this,"Không thể xóa dữ liệu từ csv" , Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (listContactsToDelete.size() > 0) {
                        for (Contact contact :
                                listContactsToDelete){
                                action.deleteContact(this, contact);
                        }
                        Load("FROM-DEVICE");
                    }
            }
            if(view == btn_refresh){
                onResume();
                Toast.makeText(MainActivity.this , "Refreshed",Toast.LENGTH_SHORT).show();
            }
            if(view == btn_readCSV){
                if(FROM=="FROM-DEVICE") {
                    FROM = "FROM-CSV";
                    Load(FROM);
                }else{
                    FROM = "FROM-DEVICE";
                    Load(FROM);
                }
            }
            if(view == btn_writeCSV){
                if(FROM=="FROM-CSV"){
                    return;
                }
                if(listContacts.size()==0){
                    Toast.makeText(MainActivity.this , "Không có dữ liệu",Toast.LENGTH_SHORT).show();
                    return;
                }
                action.saveContactsToCSV();
            }
    }
    private boolean checkExternalStorageAvailable(){
        String storage = Environment.getExternalStorageState();
        if(storage.equals(Environment.MEDIA_MOUNTED)){
                return true;
        }
        return false;
    }
}
class ContactsAdapterCustom extends RecyclerView.Adapter<ContactsAdapterCustom.DataContactHolder>{
    ArrayList<Contact> listContacts;
    Activity acc;
    IActionContacts action;
    public ContactsAdapterCustom(ArrayList<Contact> listContacts, Activity acc,IActionContacts action) {
        this.listContacts = listContacts;
        this.acc = acc;
        this.action= action;
    }
    @NonNull
    @Override
    public DataContactHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = acc.getLayoutInflater().inflate(R.layout.contact_item,null);
        return new DataContactHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull DataContactHolder holder, int position) {
        int i = position;
        Contact contact = listContacts.get(position);
        holder.tv_name.setText(contact.getName());
        holder.tv_phone.setText(contact.getPhone());
        holder.layout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

            }
        });
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (holder.checkBox.isChecked()){
                        action.addContactToRemoveList(listContacts.get(i));
                }
                else if(holder.checkBox.isChecked()==false){
                        action.deleteContactFromRemoveList(listContacts.get(i));
                }
            }
        });
    }
    @Override
    public int getItemCount() {
        return listContacts.size();
    }
    public class DataContactHolder extends RecyclerView.ViewHolder {
        private TextView tv_name,tv_phone;
        private LinearLayout layout;
        private CheckBox checkBox;
        public DataContactHolder(@NonNull View itemView) {
            super(itemView);
            layout = (LinearLayout)itemView.findViewById(R.id.layout);
            tv_name=(TextView) itemView.findViewById(R.id.tv_name);
            tv_phone=(TextView) itemView.findViewById(R.id.tv_phone);
            checkBox=(CheckBox) itemView.findViewById(R.id.checkBox);
        }
    }
}