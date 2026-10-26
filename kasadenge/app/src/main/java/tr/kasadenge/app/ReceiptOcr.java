package tr.kasadenge.app;

import android.graphics.Rect;
import com.google.mlkit.vision.text.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reassembles physical receipt rows; ML Kit block order can separate labels from prices. */
final class ReceiptOcr {
 static String orderedText(Text text) {return orderedText(text,.55);}
 static String orderedText(Text text,double tolerance) {
  List<Text.Line> lines=new ArrayList<>();
  for(Text.TextBlock block:text.getTextBlocks()) for(Text.Line line:block.getLines())
   if(line.getBoundingBox()!=null) lines.add(line);
  lines.sort(Comparator.comparingDouble(l->l.getBoundingBox().exactCenterY()));
  List<List<Text.Line>> rows=new ArrayList<>();
  for(Text.Line line:lines){
   Rect box=line.getBoundingBox(); List<Text.Line> closest=null;double best=Double.MAX_VALUE;
   for(List<Text.Line> row:rows){Rect ref=row.get(0).getBoundingBox();double delta=Math.abs(box.exactCenterY()-ref.exactCenterY());
    if(delta<=Math.min(box.height(),ref.height())*tolerance&&delta<best){best=delta;closest=row;}}
   if(closest==null){closest=new ArrayList<>();rows.add(closest);}closest.add(line);
  }
  StringBuilder out=new StringBuilder();
  for(List<Text.Line> row:rows){row.sort(Comparator.comparingInt(l->l.getBoundingBox().left));
   if(out.length()>0)out.append('\n');for(int i=0;i<row.size();i++){if(i>0)out.append(' ');out.append(row.get(i).getText());}}
  return out.length()>0?out.toString():text.getText();
 }
}
