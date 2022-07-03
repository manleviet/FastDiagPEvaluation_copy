package at.tugraz.ist.ase.cacdr.core.io;

import at.tugraz.ist.ase.cacdr.core.UserRequirement;
import at.tugraz.ist.ase.common.LoggerUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

@Slf4j
public class UserRequirementBuilder implements IUserRequirementBuildable {

        @Override
        public List<UserRequirement> buildUserRequirement(@NonNull String stringUR) {
            log.trace("{}Building user requirement from [ur={}] >>>", LoggerUtils.tab(), stringUR);
            LoggerUtils.indent();

            List<UserRequirement> userRequirement = new LinkedList<>();

            String[] tokens = stringUR.split(",");

            for (String token : tokens) {
                String[] items = token.split("=");

                String variable = items[0];
                String value = items[1];

                UserRequirement ur = UserRequirement.userRequirementBuilder().variable(variable).value(value).build();

                userRequirement.add(ur);
            }

            LoggerUtils.outdent();
            log.trace("{}Built a user requirement [ur={}]", LoggerUtils.tab(), userRequirement);
            return userRequirement;
        }
}
