package net.SoftForLife.Klich2.identification;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import net.SoftForLife.Klich2.Klich2;
import net.SoftForLife.Klich2.R;
import net.SoftForLife.Klich2.Communication.RegisterWS;
import net.SoftForLife.Klich2.model.TuserMobile;
import net.SoftForLife.Klich2.service.UserService;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TableRow;
import android.widget.TextView;

public class RegisterActivity extends Activity implements OnClickListener {
	
	private final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
	          "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
	          "\\@" +
	          "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
	          "(" +
	          "\\." +
	          "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
	          ")+"
	      );
	
	private final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(.{6,20})");

	private RegisterWS registerWS;
	
	private Button button_register_ok;
	
    private TextView mDateDisplay;
    private TableRow mPickDate1;
    private TableRow mPickDate2;
    private int mYear;
    private int mMonth;
    private int mDay;
    
    static final int DATE_DIALOG_ID = 0;
	
	public static final String LOG_TAG = "Klich_register";
	
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.register);
		button_register_ok = (Button) findViewById(R.id.button_register_ok);
		button_register_ok.setOnClickListener(this);
		
        // capture our View elements
        mDateDisplay = (TextView) findViewById(R.id.text_register_date_display);
        
        mPickDate1 = (TableRow) findViewById(R.id.tableRow1);

        // add a click listener to the button
        mPickDate1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });        
        
        mPickDate2 = (TableRow) findViewById(R.id.tableRow2);

        // add a click listener to the button
        mPickDate2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });
        
		
        // get the current date
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        
        // display the current date (this method is below)
        updateDisplay();
        
        registerWS = new RegisterWS();
	}
	
    // updates the date in the TextView
    private void updateDisplay() {
        mDateDisplay.setText(
            new StringBuilder()
                    // Month is 0 based so add 1
                    .append(mMonth + 1).append("-")
                    .append(mDay).append("-")
                    .append(mYear).append(" "));
    }
    
    // the callback received when the user "sets" the date in the dialog
    private DatePickerDialog.OnDateSetListener mDateSetListener =
            new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, 
                                      int monthOfYear, int dayOfMonth) {
                    mYear = year;
                    mMonth = monthOfYear;
                    mDay = dayOfMonth;
                    updateDisplay();
                }
    };    
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DATE_DIALOG_ID:
            return new DatePickerDialog(this,
                        mDateSetListener,
                        mYear, mMonth, mDay);
        }
        return null;
    }

	// Implement the OnClickListener callback
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		Log.i(LOG_TAG, "Click en botón register");
		
		EditText email = (EditText) this.findViewById(R.id.edit_register_email);
		EditText password = (EditText) this.findViewById(R.id.edit_register_password);
		EditText password_confirm = (EditText) this.findViewById(R.id.edit_register_password_confirm);
		EditText name = (EditText) this.findViewById(R.id.edit_register_name);
		EditText surname = (EditText) this.findViewById(R.id.edit_register_surname);
		RadioGroup sex = (RadioGroup) this.findViewById(R.id.radiogroup_register_sex);
		
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
		
		boolean password_correct = false;
		if (email_correct) {
			if ((password.getText() != null) && (!password.getText().toString().equals(""))) {
				if (checkPassword(password.getText().toString())) {
					password_correct = true;
				} else {
					// TODO Mostrar un mensaje de error indicando que el campo password debe tener una longitud mínima
					// de 6 y una máxima de 20 caracteres
				}
			} else {
				// TODO Mostrar un mensaje de error indicando que el campo password no se puede dejar vacío
				
			}
		}
		
		boolean password_confirm_correct = false;
		if (password_correct) {
			if ((password_confirm.getText() != null) && (!password_confirm.getText().toString().equals(""))) {
				if (password_confirm.getText().equals(password.getText())) {
					password_confirm_correct = true;
				} else {
					// TODO Mostrar mensaje indicando que la password y su confirmación son diferentes
				}
			} else {
				// TODO Mostrar un mensaje de error indicando que el campo password no se puede dejar vacío
				
			}
		}
		
		boolean name_correct = false;
		if (password_confirm_correct) {
			if ((name.getText() != null) && (!name.getText().toString().equals(""))) {
				name_correct = true;
			} else {
				// TODO Mostrar un mensaje de error indicando que el campo name no se puede dejar vacío
				
			}
		}
		
		boolean surname_correct = false;
		if (name_correct) {
			if ((surname.getText() != null) && (!surname.getText().toString().equals(""))) {
				surname_correct = true;
			} else {
				// TODO Mostrar un mensaje de error indicando que el campo surname no se puede dejar vacío
				
			}
		}
		
		boolean dateBirth_correct = false;
		if (surname_correct) {
			GregorianCalendar gc = new GregorianCalendar();
			gc.set(mYear, mMonth, mDay);
			
			GregorianCalendar today = new GregorianCalendar();
			
			if (gc.before(today)) {
				int anio_nacimiento = gc.get(GregorianCalendar.YEAR);
				int anio_actual = today.get(GregorianCalendar.YEAR);
				
				if ((anio_actual - anio_nacimiento) <= 120) {
					if ((anio_actual - anio_nacimiento) > 9) {
						dateBirth_correct = true;
					} else {
						//TODO Mostrar un mensaje de error indicando que menos de 10 años no es posible
					}
				} else {
					//TODO Mostrar un mensaje de error indicando que más de 120 años no es posible
				}
				
			} else {
				//TODO Mostrar un mensaje de error indicando que la fecha de nacimiento no puede ser en el futuro
			}
		}
		
		Integer sexId = null;
		boolean sex_correct = false;
		if (dateBirth_correct) {
			Integer rbid = sex.getCheckedRadioButtonId();
			
			if (rbid != null) {
				View rb = this.findViewById(rbid);
				sexId = sex.indexOfChild(rb);
				sex_correct = true;
			} else {
				//TODO Mostrar un mensaje de error indicando que que no se ha seleccionado sexo
			}
		}
		
		if (sex_correct) {
			TuserMobile user = new TuserMobile();
			user.setEmail(email.getText().toString());
			user.getUserId().setLogin(email.getText().toString());
			user.setPassword(UserService.encodeMD5(password.getText().toString()));
			user.setName(name.getText().toString());
			user.setSurname(surname.getText().toString());
			
			GregorianCalendar gc = new GregorianCalendar();
			gc.set(mYear, mMonth, mDay);
			
			user.setBirthDate(gc.getTime());
			user.setSex(sexId);
			
			registerWS.register(user);
			
			if (user.getUserId().getUserId() >= 0) {
				//UserService userService = new UserService(this);
				//userService.insertUser(user);
				
        		Intent intent = new Intent(this, Klich2.class);
                this.startActivityForResult(intent, 0x55556);
			} else {
				if (user.getUserId().getUserId() == -1) {
					// TODO Mostrar un mensaje de error indicando que el usuario ya está registrado		
				} else if (user.getUserId().getUserId() == -2) {
					// TODO Mostrar un mensaje de error indicando que ha sido un fallo inesperado		
				} else if (user.getUserId().getUserId() == -3) {
					// TODO Mostrar un mensaje de error indicando que posiblemente hay problemas con la conexión
				}
			}
			
		}
	}
	
	private boolean checkEmail(String email) {
        return EMAIL_ADDRESS_PATTERN.matcher(email).matches();
	}
	
	private boolean checkPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
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
