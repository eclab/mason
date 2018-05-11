package CDI.src.movement;



public class AgentUtil {

	public static double getMovementWill(NorthLandsMovement model, int agentType)
	{
		switch(agentType)
		{
		case 0:
			return model.parameters.urbanMovementWill;
		case 1:
			return model.parameters.ruralMovementWill;
		default:
			System.err.println("this should never happen");
			return 0.0;
		}
	}
	
	public static double getGrowthRate(NorthLandsMovement model, int agentType)
	{
		switch(agentType)
		{
		case 0:
			//return model.parameters.urbanGrowthRate;
            return model.getUrbanGrowthRate();
		case 1:
			//return model.parameters.ruralGrowthRate;
            return model.getRuralGrowthRate();
		default:
			System.err.println("this should never happen");
			return 0.0;
		}
	}
	
	public static double getSocialWeight(NorthLandsMovement model, int agentType)
	{
		switch(agentType)
		{
		case 0:
			return model.parameters.urbanSocialWeight;
		case 1:
			return model.parameters.ruralSocialWeight;
		default:
			System.err.println("this should never happen");
			return 0.0;
		}
	}
	
	
	public static double getAdjacentSocialDiscount(NorthLandsMovement model, int agentType)
	{
		switch(agentType)
		{
		case 0:
			return model.parameters.urbanAdjacentSocialDiscount;
		case 1:
			return model.parameters.ruralAdjacentSocialDiscount;
		default:
			System.err.println("this should never happen");
			return 0.0;
		}
	}

	public static double getDesirability(NorthLandsMovement model, int cellIndex, int agentType) {
		switch(agentType)
		{
		case 0:
			return model.worldAgent.urbanDesirability[cellIndex];
		case 1:
			return model.worldAgent.ruralDesirability[cellIndex];
		default:
			System.err.println("this should never happen");
			return 0.0;
		}
	}
}

