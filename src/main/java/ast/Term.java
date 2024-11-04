package ast;

public class Term {
    public long value;
    public boolean isVar;

    public Term(Long value, boolean isVar) {
        this.value = value;
        this.isVar = isVar;
    }

}
