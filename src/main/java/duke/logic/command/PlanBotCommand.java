package duke.logic.command;

import duke.exception.DukeException;
import duke.logic.CommandParams;
import duke.logic.CommandResult;
import duke.model.Expense;
import duke.model.Model;
import duke.storage.Storage;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlanBotCommand extends Command {
    private static final String name = "plan";
    private static final String description = "a reply to planBot";
    private static final String usage = "sends the user input to planBot";

    private enum SecondaryParam {
        ;

        private String name;
        private String description;

        SecondaryParam(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }


    public PlanBotCommand() {
        super(name, description, usage, Stream.of(PlanBotCommand.SecondaryParam.values())
                .collect(Collectors.toMap(s -> s.name, s -> s.description)));
    }

    @Override
    public CommandResult execute(CommandParams commandParams, Model model, Storage storage) throws DukeException {
        if (commandParams.getMainParam() != null && commandParams.getMainParam().contains("export")) {
            try {
                for (String category : model.getRecommendedBudgetPlan().getPlanBudget().keySet()) {
                    model.setCategoryBudget(category, model.getRecommendedBudgetPlan().getPlanBudget().get(category));
                }
                for (Expense recommendedExpense : model.getRecommendedBudgetPlan().getRecommendationExpenseList()) {
                    model.addExpense(recommendedExpense);
                }
                storage.saveExpenseList(model.getExpenseList());
                storage.saveBudget(model.getBudget());
                return new CommandResult("Exported successfully!", CommandResult.DisplayedPane.EXPENSE);
            } catch (NullPointerException e) {
                return new CommandResult("Nothing to export!", CommandResult.DisplayedPane.PLAN);
            }

        } else {
            model.processPlanInput(commandParams.getMainParam());
            storage.savePlanAttributes(model.getKnownPlanAttributes());
            return new CommandResult("PlanBot replied!", CommandResult.DisplayedPane.PLAN);
        }
    }
}
