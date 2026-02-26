// Question module types — mirrors backend QuestionResponse

export interface QuestionResponse {
  id: string;
  title: string;
  description: string;
  difficulty: string;
  language: string;
  type: string | null;
  level: string | null;
  image: string | null;
}
