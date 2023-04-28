## Installation
To install this plugin all you need to do is copy the jar into the plugins folder, start the server once, and modify the config files to your liking.

## Configuration
Once the plugin starts, there should be two files. `config.yml` and `quiz.yml`.
`config.yml` will contain the basic plugin config (it's contents should be self-explanatory), whereas `quiz.yml` contains the quiz questions.

### quiz.yml
This file contains a root key, `categories`, which contains the different categories, they should each have unique names.
Inside the categories, there is one property, `questions`, which is a list that contains all the categories questions.
There are three properties in each question, and those are:
- `title`: This is the prompt that will appear in chat once this question comes up.
- `type`: This indicates the type of question this is. Currently, this can be either `MultiChoice` or `Matching`.
- `answer` or `answers`: Depending on the previous property, this will either be a single answer (`Matching`, which is a String), or multiple answers (`MultiChoice`).
  - Multiple answers: These will contain two properties, `title`, which will be what the answer displays, and `valid`, which is to indicate which is the valid answer.