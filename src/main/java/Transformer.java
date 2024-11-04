import ast.EqualitySet;
import ast.Program;
import java.util.*;

public class Transformer {


    public static void setEqSet(Program p) {
        for(var r: p.rules) {
            r.equalitySet = new EqualitySet();
            for(int i = 0; i < r.body.size(); i++) {
                var currentRelation = r.body.get(i);
                for(int j = 0; j < currentRelation.ids.size(); j++) {
                    var currentTerm = currentRelation.ids.get(j);
                    if(currentTerm.isVar) {
                        r.equalitySet.setEqual(i, j, currentTerm.value);
                    } else {
                        r.equalitySet.setConstant(i, j, (int) currentTerm.value);
                    }
                }
            }
        }
    }
}
