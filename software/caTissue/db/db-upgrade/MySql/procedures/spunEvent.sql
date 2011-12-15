drop procedure if exists    spun_Event_migrate;
//
create procedure    spun_Event_migrate() 
  
  Begin   
     DECLARE counter integer default 0;
   DECLARE _stme TEXT;
  DECLARE _output2 TEXT default 'success' ;
  declare record_not_found integer default 0;
  declare form_context_id integer default 1;
  declare seq_ver long ;
  
  declare specimen_event_identifier integer ;
  declare specimen_id integer ;
  declare specimen_event_user_id integer ; 
  declare specimen_event_param_id integer ;
  declare specimen_comments varchar(100);
  declare specimen_timestamp DATE;
    declare dispo_GFORCE double;
    declare dispo_DURATION_IN_MINUTES integer;
    declare query_text varchar(255);
    declare query_text_form varchar(255);


 #-----------------------@using parameter-----------------------------
  declare activitystatus Text;
  declare sp_id integer;
  declare des_reason Text default '';
  declare s_seq_var long default 1;
  declare gforce double;
  declare time_dur integer;
  
  declare event_name varchar (100) default 'SpunEventParameters';
  #-------------------------------------------------------------------

      
   declare mig_cursor cursor   for
    select spec.identifier,
           spec.specimen_id,
           spec.event_timestamp,
           spec.user_id,
           spec.comments,
           coll.GFORCE,
           coll.DURATION_IN_MINUTES
      from    catissue_spun_event_parameters coll,
           catissue_specimen_event_param spec,
         catissue_specimen se
	where
      coll.identifier = spec.identifier and spec.specimen_id=se.identifier;
      
  declare CONTINUE HANDLER for NOT FOUND SET record_not_found = 1; 

  
  #-----------------------------query for form contex id--------------------------------------
  SELECT formContext.identifier into form_context_id FROM dyextn_abstract_form_context formContext 
              join dyextn_container dcontainer
              on formcontext.container_id = dcontainer.identifier
              join dyextn_entity ent
              on ent.identifier = dcontainer.abstract_entity_id
              join dyextn_abstract_metadata  meta
              on meta.name=event_name and meta.identifier = ent.identifier
              join dyextn_abstract_metadata  meta2
              on meta2.name = 'SpecimenEvents'
              join dyextn_entity_group eg
              on eg.identifier=meta2.identifier
              and eg.identifier  =  ent.ENTITY_GROUP_ID and formContext.Activity_Status='Active';
              
  #-----------------------------------calling function---------------------------------------------------------------        
              
              select    query_formation(event_name) into query_text;
              select query_text;
              set @query_text_form := query_text;
              select @query_text_form;
              prepare stmt from @query_text_form;
              
  #------------------------------------------------------------------------------------------------------------------       
        set counter :=1;     
  #------------------------------------------------------------------------------------------------------------------       
      open mig_cursor;
    
     
          
     read_loop: LOOP
      
      
      fetch mig_cursor into specimen_event_identifier,
                            specimen_id,
                            specimen_timestamp,
                            specimen_event_user_id,
                            specimen_comments,
                            dispo_GFORCE,
                            dispo_DURATION_IN_MINUTES;
      
      
      if record_not_found then LEAVE read_loop;
      End if;
      
      #-------------------------------------------------------------------
      INSERT IGNORE into    dyextn_abstract_record_entry
      (modified_date,activity_status,abstract_form_context_id)
      values (sysdate(),'Active',form_context_id);  
      #-------------------------------------------------------------------   
      select _output2;
      select max(identifier) into seq_ver from  dyextn_abstract_record_entry;
      select seq_ver;
      #-------------------------------------------------------------------     
      
      INSERT IGNORE into    catissue_action_app_rcd_entry(identifier)values(seq_ver);
      #select _output2;
      #-------------------------------------------------------------------
  
      INSERT IGNORE into    catissue_abstract_application
          (identifier,timestamp,user_details,comments)
      values(specimen_event_identifier,specimen_timestamp,specimen_event_user_id,specimen_comments);
      select _output2;
      #----------------------print tha all values ---------------------------------------------
       
        select  specimen_event_identifier,
                            specimen_id,
                            specimen_timestamp,
                            specimen_event_user_id,
                            specimen_comments;
       #-------------------------------------------------------------------
       
      INSERT IGNORE into    catissue_action_application
      (identifier,specimen_id,action_app_record_entry_id)
      values(specimen_event_identifier,specimen_id,seq_ver);
      #-------------------------------------------------------------------
      
   set @sp_id := specimen_event_identifier;
   set @activitystatus :='Active';
  
   set @s_seq_var :=seq_ver;
   set @gforce :=dispo_GFORCE;
   set @time_dur :=dispo_DURATION_IN_MINUTES;
    
     
   execute stmt using @time_dur,@gforce,@sp_id,@s_seq_var;     
     
      
    set counter =counter+1;
    set _stme=counter;
    select _stme;

                           
    end loop;          
    close mig_cursor; 
    #------------------------------------------------------------------
  
end;
//