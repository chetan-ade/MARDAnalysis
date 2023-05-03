package dont_submit;

public class MhpQuery {
	String var;
	String field;
	
	public MhpQuery(String var, String field) {
		super();
		this.var = var;
		this.field = field;
	}

	public String getVar() {
		return var;
	}


	public String getField() {
		return field;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Can the accesses to the field "+var+"."+field+" lead to data race?");
		
		return sb.toString();
	}
	
}