package com.dodrop.fikisha;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.dodrop.fikisha.Common.Common;
import com.dodrop.fikisha.Model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    Button btnLogin, btnRegister ;
    RelativeLayout rootLayout;

    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference(Common.user_driver_tbl);


        btnLogin = (Button)findViewById(R.id.btnLogin);
        btnRegister = (Button)findViewById(R.id.btnRegister);
        rootLayout  = (RelativeLayout)findViewById(R.id.rootLayout);


        //Events
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRegisterDialoig();

            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSigninDialog();

            }
        });

    }

    private void showSigninDialog() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("LOGIN");
        dialog.setMessage("Please Use your Email To Login");

        LayoutInflater inflater = LayoutInflater.from(this);
        View login_layout = inflater.inflate(R.layout.layout_login,null);

        final MaterialEditText edtEmail = login_layout.findViewById(R.id.edtEmail);
        final MaterialEditText edtPassword = login_layout.findViewById(R.id.edtPassword);


        dialog.setView(login_layout);

        //set button
        dialog.setPositiveButton("LOGIN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();

                btnLogin.setEnabled(false);

                // check Validation
                if (TextUtils.isEmpty(edtEmail.getText().toString()))
                {
                    Snackbar.make(rootLayout, "Please Enter Email Address", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }
                if (edtPassword.getText().toString().length() < 6 )
                {
                    Snackbar.make(rootLayout, "Password Too Short.", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }
                // Sign in to account

                final AlertDialog waitingDialog = new SpotsDialog(MainActivity.this);
                waitingDialog.show();

                auth.signInWithEmailAndPassword(edtEmail.getText().toString(), edtPassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                waitingDialog.dismiss();
                                startActivity(new Intent(MainActivity.this,Welcome.class));
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                waitingDialog.dismiss();
                                Snackbar.make(rootLayout, "Failed to Login" +e.getMessage(), Snackbar.LENGTH_SHORT)
                                 .show();

                                //active button
                                btnLogin.setEnabled(true);
                            }
                        });

            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });


        dialog.show();
    }

    private void showRegisterDialoig(){
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Register");
        dialog.setMessage("Please Use your Email To Register");

        LayoutInflater inflater = LayoutInflater.from(this);
        View register_layout = inflater.inflate(R.layout.layout_register,null);

        final MaterialEditText edtEmail = register_layout.findViewById(R.id.edtEmail);
        final MaterialEditText edtPassword = register_layout.findViewById(R.id.edtPassword);
        final MaterialEditText edtPhone = register_layout.findViewById(R.id.edtPhone);
        final MaterialEditText edtName = register_layout.findViewById(R.id.edtName);

        dialog.setView(register_layout);

        //set button
        dialog.setPositiveButton("REGISTER", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();

                // check Validation
                if (TextUtils.isEmpty(edtEmail.getText().toString()))
                {
                    Snackbar.make(rootLayout, "Please Enter Email Address", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }
                if (TextUtils.isEmpty(edtPhone.getText().toString()))
                {
                    Snackbar.make(rootLayout, "Please Enter Phone Number", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }
                if (edtPassword.getText().toString().length() < 6 )
                {
                    Snackbar.make(rootLayout, "Password Too Short.", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }
                // Register New Password
                auth.createUserWithEmailAndPassword(edtEmail.getText().toString(),edtPassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                //Save User to db
                                User user =  new User();
                                user.setEmail(edtEmail.getText().toString());
                                user.setName(edtName.getText().toString());
                                user.setPassword(edtPassword.getText().toString());
                                user.setPhone(edtPhone.getText().toString());

                                //use email to key
                                users.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(user)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Snackbar.make(rootLayout, "Registration Successful.", Snackbar.LENGTH_SHORT)
                                                        .show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Snackbar.make(rootLayout, "Registration Unsuccessful."+e.getMessage(), Snackbar.LENGTH_SHORT)
                                                        .show();
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(rootLayout, "Failed."+e.getMessage(), Snackbar.LENGTH_SHORT)
                                        .show();
                            }
                        });
            }
        });
        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialog.show();
    }
}
