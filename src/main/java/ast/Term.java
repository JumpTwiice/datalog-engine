package ast;

public class Term {
    Long value;
    boolean isVar;

    public Term(Long value, boolean isVar) {
        this.value = value;
        this.isVar = isVar;
    }

}
