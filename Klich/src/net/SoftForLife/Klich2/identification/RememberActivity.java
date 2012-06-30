package net.SoftForLife.Klich2.identification;

import java.util.regex.Pattern;

import net.SoftForLife.Klich2.Klich2;
import net.SoftForLife.Klich2.R;
import net.SoftForLife.Klich2.Communication.RememberWS;
import net.SoftForLife.Klich2.model.TuserMobile;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class RememberActivity extends Activity implements OnClickListener {

	public static final String LOG_TAG = "Klich_remember";
	
	private final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
	          "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
	          "\\@" +
	          "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
	          "(" +
	          "\\." +
	          "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
	          ")+"
	      );
	
	private Button button_remember_ok;
	private RememberWS rememberWS;
	
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.remember_user);
		button_remember_ok = (Button) this.findViewById(R.id.button_remember_ok);
		button_remember_ok.setOnClickListener(this);
		
		rememberWS = new RememberWS();
	}
	
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		Log.i(LOG_TAG, "Click en botón remember");
		
		EditText email = (EditText) this.findViewById(R.id.edit_remember_email);
		
		boolean email_correct = false;
		if ((email.getText() != null) && (!email.getText().toString().equals(""))) {
			if (checkEmail(email.getText().toString())) {
				email_correct = true;
			} else {
				// TODO Mostrar un mensaje de error indicando que el campo mail no tiene formato de correo electrónico
			}
		} else {
			// TODO Mostrar un mensaje de error indicando que el campo mail no se puede dejar vacío
		}
		
		if (email_correct) {
			TuserMobile user = new TuserMobile();
			user.setEmail(email.getText().toString());
			user.getUserId().setLogin(email.getText().toString());
			
			rememberWS.remember(user);
			
			if (user.getUserId().getUserId() >= 0) {
        		Intent intent = new Intent(this, Klich2.class);
                this.startActivityForResult(intent, 0x55556);
			} else {
				if (user.getUserId().getUserId() == -1) {
					// TODO Mostrar un mensaje de error indicando que el usuario no está registrado		
				} else if (user.getUserId().getUserId() == -2) {
					// TODO Mostrar un mensaje de error indicando que el correo de notificación no pudo ser enviado	
				} else if (user.getUserId().getUserId() == -3) {
					// TODO Mostrar un mensaje de error indicando que posiblemente hay problemas con la conexión
				}
			}
		}
	}
	
	private boolean checkEmail(String email) {
        return EMAIL_ADDRESS_PATTERN.matcher(email).matches();
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
}
