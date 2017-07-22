import bwapi.Unit;

public class UnitFSM {
	private Unit unit;
	private enum state { building, fleeing, attacking};
	private state state;
	public UnitFSM(Unit unit, state myState) {
		this.setUnit(unit);
		this.setState(myState);
	}
	public Unit getUnit() {
		return unit;
	}
	public void setUnit(Unit unit) {
		this.unit = unit;
	}
	public state getState() {
		return state;
	}
	public void setState(state state) {
		this.state = state;
	}
	
}
