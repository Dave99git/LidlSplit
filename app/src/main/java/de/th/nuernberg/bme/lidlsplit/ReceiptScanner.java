package de.th.nuernberg.bme.lidlsplit;

import android.content.Context;
import android.net.Uri;
import android.graphics.Rect;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.List;

/**
 * Helper component that runs ML Kit's on-device text recognition on a receipt
 * image and parses the result using {@link ReceiptParser}. The OCR blocks and
 * lines with their bounding boxes are logged for debugging.
 */
public class ReceiptScanner {

    /** Callback interface used to deliver the scanning result. */
    public interface Callback {
        void onSuccess(ReceiptData data, List<PurchaseItem> items);
        void onError(Exception e);
    }

    /**
     * Starts text recognition on the given image URI and parses the result.
     */
    public static void scanImage(Context context, Uri imageUri, Callback cb) {
        try {
            InputImage img = InputImage.fromFilePath(context, imageUri);
            TextRecognizer recognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(img)
                    .addOnSuccessListener(result -> handleSuccess(result, cb))
                    .addOnFailureListener(cb::onError);
        } catch (IOException e) {
            cb.onError(e);
        }
    }

    private static void handleSuccess(Text result, Callback cb) {
        for (Text.TextBlock block : result.getTextBlocks()) {
            Rect b = block.getBoundingBox();
            Log.d("ReceiptScanner", "Block: " + block.getText().replace("\n", " ")
                    + " | Box: " + b);
            for (Text.Line line : block.getLines()) {
                Log.d("ReceiptScanner", "Line: " + line.getText()
                        + " | Box: " + line.getBoundingBox());
            }
        }

        ReceiptParser parser = new ReceiptParser();
        List<PurchaseItem> items = parser.parseOcr(result);
        ReceiptData meta = parser.parse(result.getText());

        ReceiptData data = new ReceiptData(items, meta.getTotal(),
                meta.getStreet(), meta.getCity(), meta.getDateTime());
        cb.onSuccess(data, items);
    }
}
