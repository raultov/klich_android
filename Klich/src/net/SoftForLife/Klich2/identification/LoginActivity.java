package net.SoftForLife.Klich2.identification;

import net.SoftForLife.Klich2.Klich2;
import net.SoftForLife.Klich2.R;
import net.SoftForLife.Klich2.Communication.LoginWS;
import net.SoftForLife.Klich2.model.TuserMobile;
import net.SoftForLife.Klich2.service.UserService;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity implements OnClickListener {

	private Button button_login_ok;
	private LoginWS loginWS;
	
	public static final String LOG_TAG = "Klich_login";
	
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//activity = act;
		setContentView(R.layout.login);
		
		button_login_ok = (Button) this.findViewById(R.id.button_login_ok);
		
		button_login_ok.setOnClickListener(this);	
		
		loginWS = new LoginWS();
	}

	// Implement the OnClickListener callback
	@Override
	public void onClick(View v) {
		Log.i(LOG_TAG, "Click en botón login");
		
		EditText email = (EditText) this.findViewById(R.id.edit_login_email);
		EditText password = (EditText) this.findViewById(R.id.edit_login_password);
		boolean email_correct = false;
		
		if ((email.getText() != null) && (!email.getText().toString().equals(""))) {
			if ((email.getText().toString().contains("@")) && (email.getText().toString().contains("."))) {
				email_correct = true;
			} else {
				// TODO Mostrar un mensaje de error indicando que el campo mail no tiene formato de correo electrónico
			}
		} else {
			// TODO Mostrar un mensaje de error indicando que el campo mail no se puede dejar vacío
		}
		
		boolean password_correct = false;
		if (email_correct) {
			if ((password.getText() != null) && (!password.getText().toString().equals(""))) {
				password_correct = true;
			} else {
				// TODO Mostrar un mensaje de error indicando que el campo password no se puede dejar vacío
				
			}
		}
		
		if (password_correct) {
			TuserMobile user = new TuserMobile();
			user.setEmail(email.getText().toString());
			user.getUserId().setLogin(email.getText().toString());
			user.setPassword(UserService.encodeMD5(password.getText().toString()));
			
			loginWS.login(user);
			
			if (user.isRegistered()) {
				UserService userService = new UserService(this);
				userService.insertUser(user);
				
        		Intent intent = new Intent(this, Klich2.class);
                this.startActivityForResult(intent, 0x55556);
				
			} else {
				
				if (user.getUserId().getUserId() == -1) {
					// TODO Mostrar un mensaje de error indicando que el usuario no está registrado
				} else if (user.getUserId().getUserId() == -2) {
					// TODO Mostrar un mensaje de error indicando que ha sido un fallo inesperado
				} else if (user.getUserId().getUserId() == -3) {
					// TODO Mostrar un mensaje de error indicando que posiblemente hay problemas con la conexión
				}
			}
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
};
