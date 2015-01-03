package dmeeuwis.masterlibrary;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class PurchaseDialog extends DialogFragment {

	private final static String MESSAGE_KEY = "message_key";
	
    public enum DialogMessage { 
    							LOCK_BUTTON("The katakana and higher level kanji sets are locked, and require a one-time purchase to unlock. 10 characters are available from each set as a sample.\n\nUnlocking will give you access to all characters, and support the further development of this application."), 
    							START_OF_LOCKED_SET("The katakana and higher level kanji sets are locked, and require a one-time purchase to unlock. 10 characters are available from each set as a sample.\n\nUnlocking will give you access to all characters, and support the further development of this application."), 
    							END_OF_LOCKED_SET("You reached the end of the sample characters for this set. To access the complete set, a one-time unlock is required.\n\nUnlocking will give you access to all characters, and support the further development of this application."),
    							LOCKED_CHARACTER("This character is part of the locked set, and requires a one-time purchase to unlock. This purchase will unlock all characters, and support the further development of this application.");

    							public final String message;
    
    							DialogMessage(String message){
    								this.message = message;
    							}
    				};
    				
    public static PurchaseDialog make(DialogMessage mess){
    	Bundle b = new Bundle();
    	b.putString(MESSAGE_KEY, mess.name());
    	PurchaseDialog pd = new PurchaseDialog();
    	pd.setArguments(b);
    	return pd;
    }
    				
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	this.setRetainInstance(true);
    	
    	final DialogMessage mess = DialogMessage.valueOf(this.getArguments().getString(MESSAGE_KEY));
    	final LockChecker lc = ((LockCheckerHolder)getActivity()).getLockChecker();
    	
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
        		.setTitle("Unlock Full Version?")
        		.setMessage(mess.message)
        		.setPositiveButton("Unlock Full Version", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                      lc.runPurchase();
                      PurchaseDialog.this.dismiss();
                   }
               })
               .setNegativeButton("Stay with Free Version", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       PurchaseDialog.this.dismiss();
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();	
    }
}