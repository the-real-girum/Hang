from google.appengine.ext import db
from push import push_to_user

import webapp2
import json
import urllib2


# Our model class.
class User(db.Model):
    first_name = db.StringProperty()
    last_name = db.StringProperty()
    gcm_registration_ids = db.ListProperty(str)
    
    incoming_broadcasts = db.ListProperty(db.Key)
    outgoing_broadcasts = db.ListProperty(db.Key)
    
    status_color = db.StringProperty()
    status_description = db.StringProperty()
    status_expiration_date = db.StringProperty()  # TODO: Change to DateTimeProperty()
    
    proposal_description = db.StringProperty()
    proposal_location = db.StringProperty()
    proposal_time = db.StringProperty()  # TODO: Change to DateTimeProperty()
    proposal_interested = db.ListProperty(db.Key)
    proposal_confirmed = db.ListProperty(db.Key)
    library = db.ListProperty(db.Key)

    # We need the get_partial_json to be able to print out just jids of the int and conf
    # users so jid String Lists seems the way to go. The other alternative I can think of
    # is to embarassingly request the jids using the int/conf Keys programmatically.
    #
    # (Note from Girum): You should indeed make this property a db.ListProperty(db.Key)
    # instead of db.ListProperty(str) like you have now. db.Key objects contain their 
    # string name within them as a field, so it is very cheap to retrieve their JIDs 
    # programmatically. Use <key_object>.name() to cheaply retrieve the JID from the Key 
    # object (example in User's GET request handler, when I convert incoming_broadcast_keys
    # to JSON objects.
    proposal_interested_jids = db.ListProperty(str)
    proposal_confirmed_jids = db.ListProperty(str)

    proposals_seen_jids = db.ListProperty(str)

    unseen_chats = db.ListProperty(str)

    # This is if we want to keep simple stats per user 
    # total_proposals = db.IntegerProperty()
    # total_availabilities = db.IntegerProperty()
    # total_statuses = db.IntegerProperty()

    def get_partial_json(self):
        return {
            'jid': self.key().name(),
            'fn': self.first_name,
            'ln': self.last_name,
            'color': self.status_color,
            'exp': self.status_expiration_date,
            'text': self.status_description,
            'des': self.proposal_description,
            'loc': self.proposal_location,
            'time': self.proposal_time,
            'int' : self.proposal_interested_jids,
            'conf' : self.proposal_confirmed_jids
        }

    # This is only used when Interested users of Incoming Broadcasters are 
    # added into the library in the UserRequestHandler (approx line 91)
    def get_stranger_json(self):
        return {
            'jid': self.key().name(),
            'fn': self.first_name,
            'ln': self.last_name,
            'color': self.status_color,
            'exp': self.status_expiration_date
        }
    

# The RequestHandler for a User    
class UserRequestHandler(webapp2.RequestHandler):
    def get(self, jid):
        # Set this method to return JSON instead of normal text
        self.response.headers['Content-Type'] = 'application/json'
        
        # Create the Key for the Entity we want using the JID that was passed in.               
        key_user = db.Key.from_path('User', jid)
   
        # Perform the query for the Entity using that Key.
        user = db.get(key_user)

        if user is None:
            self.response.write("User doesn't exist")
            return

        
        # Perform two more queries for the keys of the user's incoming_broadcasts and outgoing_broadcasts
        incoming_broadcasts_keys = db.get(user.incoming_broadcasts)
        outgoing_broadcasts_keys = db.get(user.outgoing_broadcasts)
        
        # Perform queries for the user's proposal's interested and confirmed lists
        interested_users = db.get(user.proposal_interested)
        confirmed_users = db.get(user.proposal_confirmed)

        # JSON library that will store a mapping of jids to full JSON strings
        library_json = {}
        
        # Convert the incoming broadcasts to JSON objects
        incoming_broadcasts_jids = []
        for incoming_broadcast in incoming_broadcasts_keys:

            # Get the jid of the Incoming Broadcaster
            inc_jid = incoming_broadcast.key().name()
            
            # Add their jid in the Incoming List 
            incoming_broadcasts_jids.append(inc_jid)

            # If their JSON info isn't in the Library, add it
            if inc_jid not in library_json:
                library_json[inc_jid] = incoming_broadcast.get_partial_json()

            # Add the Interested Users of the Incoming Broadcaster to our library
            # TODO: Later we should only be retrieving their fn, ln, jid, (maybe their icon?) 
            for user_key in incoming_broadcast.proposal_interested:

                # Make sure the Interested User isn't 'default' User 
                if user_key != key_user:
                    # Get their User object
                    int_user = db.get(user_key)

                    # Get their jid
                    int_jid = user_key.name()

                    # If they're not in the library, add them
                    if int_jid not in library_json:
                        library_json[int_jid] = int_user.get_partial_json()

        # Convert the outgoing broadcasts to JSON objects
        outgoing_broadcasts_jids = []
        for outgoing_broadcast in outgoing_broadcasts_keys:
            out_jid = outgoing_broadcast.key().name()
            outgoing_broadcasts_jids.append(out_jid)
            if out_jid not in library_json:
                library_json[out_jid] = outgoing_broadcast.get_stranger_json()
        
        # Convert the interested users to JSON objects    
        interested_users_jids = []
        for interested_user in interested_users:
            int_jid = interested_user.key().name()
            interested_users_jids.append(int_jid)
            if int_jid not in library_json:
                library_json[int_jid] = interested_user.get_stranger_json()
            
        # Convert the confirmed users to JSON objects    
        confirmed_users_jids = []
        for confirmed_user in confirmed_users:
            conf_jid = confirmed_user.key().name()
            confirmed_users_jids.append(conf_jid)
            if conf_jid not in library_json:
                library_json[conf_jid] = confirmed_user.get_stranger_json()
        
        # Format the user into a JSON object
        user_json_object = {
            'jid': user.key().name(),
            'fn': user.first_name,
            'ln': user.last_name,
            'inc': incoming_broadcasts_jids,
            'out': outgoing_broadcasts_jids,
            'color': user.status_color,
            'text' : user.status_description,
            'exp': user.status_expiration_date,
            'des': user.proposal_description,
            'loc': user.proposal_location,
            'time': user.proposal_time,
            'int': interested_users_jids,
            'conf': confirmed_users_jids,
            'seen' : user.proposals_seen_jids,
            'lib' : library_json
        }
        
        # Output the formatted JSON object
        self.response.write(json.dumps(user_json_object, separators=(',', ':')))
        
    def put(self, jid):
        try:
            # Set this method to return JSON instead of normal text
            self.response.headers['Content-Type'] = 'application/json'
            
            # Grab the PUT request parameters and put them into variables.
            param_first_name = self.request.get('fn')
            param_last_name = self.request.get('ln')
            param_reg_id = self.request.get('regid')

            # Check if User already exists
            if db.get(db.Key.from_path('User', jid)) != None:
                user = db.get(db.Key.from_path('User', jid))
                # If the device registration id is a new one, add it. 
                if param_reg_id not in user.gcm_registration_ids:
                    user.gcm_registration_ids.append(param_reg_id)
                    user.put()
                    self.response.write("Added new gcm_registration_id " + param_reg_id)
                else:                    
                    self.response.write(param_first_name + " already exists!\n")
            else:
                # Make a User object. His Key should be his JID.
                user = User(key_name=jid,
                             first_name=param_first_name,
                             last_name=param_last_name)
                user.gcm_registration_ids.append(param_reg_id)
                
                # Save the new User into the datastore.
                user.put()
                
                # Tell the user.
                self.response.write(json.dumps(user.get_partial_json(), separators=(',', ':')))
            
        except (TypeError, ValueError):
            # If we couldn't grab the PUT request parameters, then show an error.
            self.response.write('Invalid inputs: Couldn\'t grab the PUT request parameters.\n')
            return
       


class NudgeRequestHandler(webapp2.RequestHandler):
    def post(self, jid):
        try:
            # Grab the POST request parameters and put them into variables.
            param_target = self.request.get('target')

            key_broadcaster_jid = db.Key.from_path('User', jid)
            key_broadcastee_jid = db.Key.from_path('User', param_target)

            broadcaster = db.get(key_broadcaster_jid)
            broadcastee = db.get(key_broadcastee_jid)

            self.response.write("Nudging " + broadcastee.first_name + ".")

            push_to_user(broadcastee, broadcaster, 'nudge')

            self.response.write("Nudge successful")

        except (TypeError, ValueError):
            # If we couldn't grab the PUT request parameters, then show an error.
            self.response.write('Invalid inputs: Couldn\'t grab the PUT request parameters.\n')
            return