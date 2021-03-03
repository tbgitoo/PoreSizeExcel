package excel_macro.tools;

public class PathHandler {

	public static final String[] collaborators=new String[]{"Thomas Documents"};
	
	public static final String defaultCollaborator=collaborators[0];
	public static final String[] root_path_batch_reports=new String[]{
				"/Users/thomasbraschler/Documents"
			};
	
	public static int getCollaboratorIndex(String collaborator)
	{
		int ind=-1;
		for(int collInd=0; collInd<collaborators.length; collInd++)
		{
			if(collaborators[collInd].equals(collaborator))
			{
				ind=collInd;
			}
		}
		
		return ind;
	}
	
	public static String get_root_path_batch_report(String collaborator)
	{
		int collIndex=getCollaboratorIndex(collaborator);
		
		// By default return first listed root path if collaborator not found
		if(collIndex<0)
		{
			return root_path_batch_reports[0];
		}
		
		return root_path_batch_reports[collIndex];
	}
	
	
	
}
